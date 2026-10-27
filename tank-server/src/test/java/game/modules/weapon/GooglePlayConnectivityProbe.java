package game.modules.weapon;

import com.google.gson.JsonObject;
import framework.SystemConfigData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;

/** Manual read-only probe through the same Java OAuth/HTTP code as production. Not a JUnit test. */
public final class GooglePlayConnectivityProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: configRoot reportedOrderId");
        }
        SystemConfigData.load(args[0], null);
        GooglePlayPurchaseVerifier verifier = new GooglePlayPurchaseVerifier();
        Method request = GooglePlayPurchaseVerifier.class.getDeclaredMethod("requestJson", String.class,
                String.class, String.class);
        request.setAccessible(true);
        String packageName = SystemConfigData.getConfig("googlePlayPackageName", "");
        String base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
                + URLEncoder.encode(packageName, "UTF-8");
        try {
            JsonObject order = (JsonObject) request.invoke(verifier, "GET", base + "/orders/"
                    + URLEncoder.encode(args[1], "UTF-8"), null);
            if (!args[1].equals(order.get("orderId").getAsString())) {
                throw new IllegalStateException("order identity mismatch");
            }
            // The token stays in process memory. Never send it as a command-line argument.
            JsonObject purchase = (JsonObject) request.invoke(verifier, "GET", base
                    + "/purchases/productsv2/tokens/"
                    + URLEncoder.encode(order.get("purchaseToken").getAsString(), "UTF-8"), null);
            JsonObject summary = new JsonObject();
            summary.addProperty("javaGoogleReachable", true);
            summary.addProperty("orderId", args[1]);
            summary.add("orderState", order.get("state"));
            summary.addProperty("testPurchase", purchase.has("testPurchaseContext"));
            summary.add("purchaseStateContext", purchase.get("purchaseStateContext"));
            System.out.println(summary);
        } catch (InvocationTargetException e) {
            // Do not print exception messages: request URLs may contain a purchase token.
            System.out.println("{\"javaGoogleReachable\":false,\"errorType\":\""
                    + e.getCause().getClass().getSimpleName() + "\"}");
            System.exit(1);
        }
    }
}
