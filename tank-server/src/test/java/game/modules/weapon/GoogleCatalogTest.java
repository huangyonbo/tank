package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import framework.game.ICfgReader;
import framework.game.IKernel;
import game.custommsg.CustomMsg;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GoogleCatalogTest {
    @Test
    public void realCatalogReturnsSixPositiveGoldProductsAndKeepsGoodConfigOnBadReload() throws Exception {
        NodeList items = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File("config/res/GooglePlay/GooglePlayProducts.xml")).getElementsByTagName("item");
        boolean[] corrupt = {false};
        ICfgReader cfg = (ICfgReader) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICfgReader.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getItemCount")) return items.getLength();
                    Element item = (Element) items.item((Integer) args[0]);
                    String key = (String) args[1];
                    String value = item.getAttribute(key);
                    if (method.getReturnType() == int.class) return Integer.parseInt(value);
                    if (method.getReturnType() == long.class) return corrupt[0] && key.equals("Gold") ? 0L : Long.parseLong(value);
                    if (method.getReturnType() == boolean.class) return Boolean.parseBoolean(value);
                    return value;
                });
        JSONObject[] response = {null};
        IKernel kernel = (IKernel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKernel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("loadXmlConfig")) return cfg;
                    if (method.getName().equals("response")) {
                        response[0] = JSON.parseObject(CustomMsg.String.parseFrom((byte[]) args[2]).getValue());
                    }
                    return null;
                });
        ShopModule shop = new ShopModule();
        try {
            shop.RefreshCfg(kernel, "res/GooglePlay/GooglePlayProducts.xml");
            shop.OnGooglePlayProducts(kernel, null, 10020, 1, null);
            Map<String, Long> expected = new HashMap<>();
            expected.put("google_gold_12", 600000L);
            expected.put("google_gold_28", 1400000L);
            expected.put("google_gold_50", 2500000L);
            expected.put("google_gold_108", 5400000L);
            expected.put("google_gold_328", 16400000L);
            expected.put("google_gold_648", 32400000L);
            assertEquals(expected, products(response[0]));
            corrupt[0] = true;
            shop.RefreshCfg(kernel, "res/GooglePlay/GooglePlayProducts.xml");
            shop.OnGooglePlayProducts(kernel, null, 10020, 1, null);
            assertEquals(expected, products(response[0]));
        } finally {
            shop.onDestroy();
        }
    }

    private static Map<String, Long> products(JSONObject response) {
        assertEquals(0, response.getIntValue("code"));
        JSONArray products = response.getJSONArray("products");
        Map<String, Long> actual = new HashMap<>();
        for (int i = 0; i < products.size(); i++) {
            JSONObject product = products.getJSONObject(i);
            assertEquals(2, product.getIntValue("rewardVersion"));
            assertEquals("PlayerMoney", product.getString("balanceProperty"));
            actual.put(product.getString("productId"), product.getLong("gold"));
        }
        return actual;
    }
}
