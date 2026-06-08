package pub.modules;

import framework.pub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ICfgReader;
import framework.game.ValueType;

import java.util.Arrays;

/**
 * 商城公共数据模块
 *
 * @author MaDi
 */
public class StoreModule implements IPubModule {

    private static Logger logger = LoggerFactory.getLogger(StoreModule.class);

    private static final String STORE_DIAMOND = "StoreDiamond";
    private static final String STORE_GOLD = "StoreGold";
    private static final String STORE_BATTERY = "StoreBattery";
    private static final String STORE_EXCHANGE = "StoreExchange";
    private static final String STORE_EXCHANGE_CARD = "StoreExchangeCard";
    private static final String STORE_EXCHANGE_VIP_INTEGRAL = "StoreExchangeVipIntegral";
    private static final String STORE_COUPONS = "StoreCoupons";

    enum SaleStaus {
        NEW, RECOMMEND, HOT
    }

    enum ITEM_COL {
        COL_ITEM_ID, COL_PRICE, COL_SALE_STATUS,

        COL_END
    }

    enum ITEM_BATTERY_COL {
        COL_ITEM_ID, COL_PRICE, COL_SALE_STATUS, COL_PROPERTIES, COL_SHOW,

        COL_END
    }

    enum ITEM_EXCHANGE_COL {
        COL_ITEM_ID, COL_PRICE, COL_SALE_STATUS, COL_SALES_COUNT, COL_STOCK, COL_VIRTUAL, COL_ITEM_NAME, COL_COST_MATERIAL,
        COL_PROPERTIES, COL_VERSION, COL_LIMIT, COL_RESET, COL_COST_ITEM_ID, COL_SWITCH, COL_SEQUENCE,

        COL_END
    }

    enum ITEM_EXCHANGE_CARD_COL {
        COL_ITEM_ID, COL_PRICE, COL_LIMIT, COL_VIRTUAL, COL_ITEM_NAME, COL_COST_MATERIAL, COL_PROPERTIES, COL_SALES_COUNT, COL_STOCK, COL_END
    }

    enum BUY_GOODS_RESULT {
        SUCCESS, STOCK_LACK, GOODS_NOT_EXIST, FAILED, VIP_LIMIT
    }

    enum ITEM_STONE_COL {
        COL_ITEM_ID, COL_PRICE, COL_STOCK, COL_VERSION,

        COL_END
    }

    enum ITEM_EXCHANGE_VIP_INTEGRAL_COL {
        COL_ITEM_ID, COL_PRICE, COL_SALES_COUNT, COL_STOCK, COL_VIRTUAL, COL_ITEM_NAME, COL_COST_MATERIAL, COL_VERSION, COL_CONDITION, COL_WEEK_RESET, COL_COST_ITEM_ID, COL_END
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean onInit(IPubKernel pubKernel) {
        pubKernel.regOnLoadEvent("pubdata", this, "OnPubdataLoad");
        pubKernel.regServerRequest(ServerMsgDef.PUBMSG_BUY_GOODS.ordinal(), this, "OnBuyGoods");
        pubKernel.regServerRequest(ServerMsgDef.PUBMSG_BUY_STONE.ordinal(), this, "OnBuyStone");
        pubKernel.regServerMsg(ServerMsgDef.PUBMSG_ON_CHANGE_DAY.ordinal(), this, "OnChangeDay");
        pubKernel.regServerMsg(ServerMsgDef.PUBMSG_ON_CHANGE_WEEK.ordinal(), this, "OnChangeWeek");
        pubKernel.regServerRequest(ServerMsgDef.B2P_UPDATE_EXCHANGE.ordinal(), this, "OnUpdateExchange");
        pubKernel.regServerRequest(ServerMsgDef.B2P_UPDATE_STORE.ordinal(), this, "OnUpdateStore");
        return true;
    }

    public void OnChangeDay(IPubKernel pubKernel, int serid, int msgid, byte[] data)
            throws InvalidProtocolBufferException {
        // 重新加载兑换库存
        logger.info("reload STORE_EXCHANGE stock");
        ICfgReader storeReader = pubKernel.loadXmlConfig("res/StoreItems/" + STORE_EXCHANGE + ".xml");
        IPubData pubData = pubKernel.getPubData(STORE_EXCHANGE, false);
        if (null != pubData) {
            IPubRecord record = pubData.getRecord("Record");
            boolean needSave = false;
            for (int i = 0; i < record.getRows(); i++) {
                String itemId = record.getString(i, 0);
                int configRow = hasItemId(storeReader, itemId);
                if (-1 != configRow) {
                    if (record.getBool(i, ITEM_EXCHANGE_COL.COL_RESET.ordinal())) {
                        // 需要每日重置库存的物品
                        record.setValue(i, ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal(), 0);
                        record.setValue(i, ITEM_EXCHANGE_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
                        needSave = true;
                    }
                }
            }
            if (needSave) {
                pubKernel.storePubData(pubData);
            }
        }
    }

    public void OnChangeWeek(IPubKernel pubKernel, int serid, int msgid, byte[] data) {
        // 重置灵石兑换库存
        ICfgReader storeReader = pubKernel.loadXmlConfig("res/StoreItems/StoreStone.xml");
        IPubData pubData = pubKernel.getPubData("StoreStone", false);
        IPubRecord record = pubData.getRecord("Record");
        for (int i = 0; i < record.getRows(); i++) {
            String itemId = record.getString(i, 0);
            int configRow = hasItemId(storeReader, itemId);
            if (-1 == configRow) {// 如果配置文件中没有此物品
                record.removeRow(i);
                i--;
            } else {
                record.setValue(i, ITEM_STONE_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
            }
        }
        // 重置贵族积分兑换库存
        storeReader = pubKernel.loadXmlConfig("res/StoreItems/" + STORE_EXCHANGE_VIP_INTEGRAL + ".xml");
        pubData = pubKernel.getPubData(STORE_EXCHANGE_VIP_INTEGRAL, false);
        record = pubData.getRecord("Record");
        for (int i = 0; i < record.getRows(); i++) {
            boolean weekReset = record.getBool(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_WEEK_RESET.ordinal());
            if (weekReset) {
                String itemId = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_ID.ordinal());
                int configRow = hasItemId(storeReader, itemId);
                if (-1 == configRow) {// 如果配置文件中没有此物品
                    record.removeRow(i);
                    i--;
                } else {
                    record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal(), 0);
                    record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
                }
            }
        }
        pubKernel.storePubData(pubData);
    }

    void OnBuyStone(IPubKernel pubKernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.BuyStone msg = ServerMsg.BuyStone.parseFrom(data);
        String item = msg.getItem();
        int count = msg.getCount();
        ServerMsg.StoreBuyGoodsResult.Builder storeBuyGoodsResult = ServerMsg.StoreBuyGoodsResult.newBuilder();
        do {
            IPubData pubData = pubKernel.getPubData("StoreStone", false);
            if (pubData == null) {
                storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.FAILED.ordinal());
                break;
            }
            IPubRecord record = pubData.getRecord("Record");
            if (record == null) {
                storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.FAILED.ordinal());
                break;
            }
            int pos = record.findRow(0, ITEM_STONE_COL.COL_ITEM_ID.ordinal(), item);
            if (pos == -1) {
                storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.GOODS_NOT_EXIST.ordinal());
                break;
            }
            int stock = record.getInt(pos, ITEM_STONE_COL.COL_STOCK.ordinal());
            if (stock < count) {
                storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.STOCK_LACK.ordinal());
                break;
            }
            record.setValue(pos, ITEM_STONE_COL.COL_STOCK.ordinal(), stock - count);
            storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.SUCCESS.ordinal());
            pubKernel.storePubData(pubData);
        } while (false);
        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
    }

    public void OnBuyGoods(IPubKernel pubKernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.StoreBuyGoodsResult.Builder storeBuyGoodsResult = ServerMsg.StoreBuyGoodsResult.newBuilder();
        ServerMsg.StoreBuyGoods storeBuyGoods = ServerMsg.StoreBuyGoods.parseFrom(data);
        IPubData pubData = pubKernel.getPubData(storeBuyGoods.getPubName(), false);
        String itemId = storeBuyGoods.getItemId();
        int buyCount = storeBuyGoods.getBuyCount();
        int playerId = storeBuyGoods.getPlayerId();
        int channelId = storeBuyGoods.getChannelId();
        int vipLevel = storeBuyGoods.getVipLevel();
        String fullName = storeBuyGoods.hasFullName() ? storeBuyGoods.getFullName() : "";
        String qq = storeBuyGoods.hasQq() ? storeBuyGoods.getQq() : "";
        String wechat = storeBuyGoods.hasWechat() ? storeBuyGoods.getWechat() : "";
        String cellphone = storeBuyGoods.hasCellphone() ? storeBuyGoods.getCellphone() : "";
        String address = storeBuyGoods.hasAddress() ? storeBuyGoods.getAddress() : "";
        String playerName = storeBuyGoods.getPlayerName();
        IPubRecord record = pubData.getRecord("Record");
        int rows = record.getRows();
        for (int i = 0; i < rows; i++) {
            if (storeBuyGoods.getPubName().equals(STORE_EXCHANGE_CARD)) {
                String id = record.getString(i, ITEM_EXCHANGE_CARD_COL.COL_ITEM_ID.ordinal());
                if (id.equals(itemId)) {
                    int salesCount = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_SALES_COUNT.ordinal());
                    int stock = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_STOCK.ordinal());
                    if (salesCount + buyCount > stock) {// 检查库存是否足够
                        logger.error("lack of stock can not buy goods:" + itemId);
                        storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.STOCK_LACK.ordinal());
                        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                        return;
                    }
                    record.setValue(i, ITEM_EXCHANGE_CARD_COL.COL_SALES_COUNT.ordinal(), salesCount + buyCount);
                    storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.SUCCESS.ordinal());
                    pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                    int price = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_PRICE.ordinal());
                    boolean isVirtual = record.getBool(i, ITEM_EXCHANGE_CARD_COL.COL_VIRTUAL.ordinal());
                    String itemName = record.getString(i, ITEM_EXCHANGE_CARD_COL.COL_ITEM_NAME.ordinal());
                    String costMaterial = record.getString(i, ITEM_EXCHANGE_CARD_COL.COL_COST_MATERIAL.ordinal());
                    if (!isVirtual) {// 如果是实物则增加订单信息到数据库给后台查看及操作
                        // 玩家id,所属渠道,玩家昵称,物品名称,兑换消耗材料名称,是否发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, false, fullName, qq, wechat, cellphone, address,vipLevel);
                    } else { // 虚拟物品默认发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, true, fullName, qq, wechat, cellphone, address,vipLevel);
                    }
                    pubKernel.storePubData(pubData);
                    return;
                }
            } else if (STORE_EXCHANGE_VIP_INTEGRAL.equals(storeBuyGoods.getPubName())) {
                String id = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_ID.ordinal());
                if (id.equals(itemId)) {
                    String condition = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_CONDITION.ordinal());
                    int vipLimit = Integer.parseInt(condition);
                    if (vipLevel < vipLimit) {
                        logger.error("vipLevel can not buy goods, current vip is {}, the limit is {}", vipLevel, vipLimit);
                        storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.VIP_LIMIT.ordinal());
                        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                        return;
                    }
                    int salesCount = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal());
                    int stock = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_STOCK.ordinal());
                    if (salesCount + buyCount > stock) {// 检查库存是否足够
                        logger.error("lack of stock can not buy goods:" + itemId);
                        storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.STOCK_LACK.ordinal());
                        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                        return;
                    }

                    record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal(), salesCount + buyCount);
                    storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.SUCCESS.ordinal());
                    pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());

                    int price = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_PRICE.ordinal());
                    boolean isVirtual = record.getBool(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VIRTUAL.ordinal());
                    String itemName = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_NAME.ordinal());
                    String costMaterial = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_COST_MATERIAL.ordinal());
                    if (!isVirtual) {// 如果是实物则增加订单信息到数据库给后台查看及操作
                        // 玩家id,所属渠道,玩家昵称,物品名称,兑换消耗材料名称,是否发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, false, fullName, qq, wechat, cellphone, address,vipLevel);
                    } else { // 虚拟物品默认发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, true, fullName, qq, wechat, cellphone, address,vipLevel);
                    }
                    pubKernel.storePubData(pubData);
                    return;
                }
            } else {
                String id = record.getString(i, ITEM_EXCHANGE_COL.COL_ITEM_ID.ordinal());
                if (id.equals(itemId)) {
                    int salesCount = record.getInt(i, ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal());
                    int stock = record.getInt(i, ITEM_EXCHANGE_COL.COL_STOCK.ordinal());
                    if (salesCount + buyCount > stock) {// 检查库存是否足够
                        logger.error("lack of stock can not buy goods:" + itemId);
                        storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.STOCK_LACK.ordinal());
                        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                        return;
                    }
                    record.setValue(i, ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal(), salesCount + buyCount);
                    storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.SUCCESS.ordinal());
                    pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
                    int price = record.getInt(i, ITEM_EXCHANGE_COL.COL_PRICE.ordinal());
                    boolean isVirtual = record.getBool(i, ITEM_EXCHANGE_COL.COL_VIRTUAL.ordinal());
                    String itemName = record.getString(i, ITEM_EXCHANGE_COL.COL_ITEM_NAME.ordinal());
                    String costMaterial = record.getString(i, ITEM_EXCHANGE_COL.COL_COST_MATERIAL.ordinal());
                    if (!isVirtual) {//如果是实物则增加订单信息到数据库给后台查看及操作
                        //玩家id,所属渠道,玩家昵称,物品名称,兑换消耗材料名称,是否发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, false, fullName, qq, wechat, cellphone, address,vipLevel);
                    } else {// 虚拟物品默认发货成功
                        pubKernel.addOrder(playerId, channelId, playerName, itemName, (price * buyCount) + costMaterial, true, fullName, qq, wechat, cellphone, address,vipLevel);
                    }
                    pubKernel.storePubData(pubData);
                    return;
                }
            }
        }
        storeBuyGoodsResult.setResultCode(BUY_GOODS_RESULT.GOODS_NOT_EXIST.ordinal());
        pubKernel.responseServer(reqid, storeBuyGoodsResult.build().toByteArray());
    }

    void OnPubdataLoad(IPubKernel pubKernel, String none) {
        loadPubData(pubKernel, STORE_DIAMOND);
        loadPubData(pubKernel, STORE_DIAMOND + "Ios");
        loadPubData(pubKernel, STORE_GOLD);
        loadPubData(pubKernel, STORE_GOLD + "Ios");
        loadPubData(pubKernel, STORE_BATTERY);
        loadPubData(pubKernel, STORE_EXCHANGE);
        loadPubData(pubKernel, STORE_EXCHANGE_CARD);
        loadPubData(pubKernel, STORE_EXCHANGE_VIP_INTEGRAL);
        loadPubData(pubKernel, STORE_COUPONS);
        loadPubData(pubKernel, STORE_COUPONS + "Ios");
        loadStoneStoreData(pubKernel);
    }

    void loadStoneStoreData(IPubKernel pubKernel) {
        ICfgReader storeReader = pubKernel.loadXmlConfig("res/StoreItems/StoreStone.xml");
        int storeItemCount = storeReader.getItemCount();
        IPubData pubData = pubKernel.getPubData("StoreStone", false);
        if (null == pubData) {
            pubData = new PubData("StoreStone");
            IPubRecord record = pubData.getRecord("Record");
            if (null == record) {
                record = pubData.addRecord("Record", ITEM_STONE_COL.COL_END.ordinal(), 50, true);
                record.setColType(ITEM_STONE_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_STONE_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_STONE_COL.COL_STOCK.ordinal(), ValueType.INT);
                record.setColType(ITEM_STONE_COL.COL_VERSION.ordinal(), ValueType.INT);
            }
            for (int i = 0; i < storeItemCount; i++) {
                String id = storeReader.getString(i, "Id");
                int price = storeReader.getInt(i, "Price");
                int stock = storeReader.getInt(i, "Stock");
                int version = storeReader.getInt(i, PLAYER_PROPERTY_VERSION);
                record.addRow(id, price, stock, version);
            }
        } else {
            IPubRecord record = pubData.getRecord("Record");
            // 查找是否有删除的物品
            for (int i = 0; i < record.getRows(); i++) {
                String itemId = record.getString(i, 0);
                int configRow = hasItemId(storeReader, itemId);
                if (-1 == configRow) {// 如果配置文件中没有此物品
                    record.removeRow(i);
                    i--;
                } else {// 如果配置中有此物品
                    int dbVersion = record.getInt(i, ITEM_STONE_COL.COL_VERSION.ordinal());
                    int configVersion = storeReader.getInt(configRow, PLAYER_PROPERTY_VERSION);
                    if (dbVersion != configVersion) {
                        record.setValue(i, ITEM_STONE_COL.COL_VERSION.ordinal(), configVersion);
                        record.setValue(i, ITEM_STONE_COL.COL_PRICE.ordinal(), storeReader.getInt(configRow, "Price"));
                        record.setValue(i, ITEM_STONE_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
                    }
                }
            }
            // 查找是否有新增的物品
            for (int i = 0; i < storeItemCount; i++) {
                int row = record.findRow(0, 0, storeReader.getString(i, "Id"));
                if (-1 == row) {
                    String id = storeReader.getString(i, "Id");
                    int price = storeReader.getInt(i, "Price");
                    int stock = storeReader.getInt(i, "Stock");
                    int version = storeReader.getInt(i, PLAYER_PROPERTY_VERSION);
                    record.addRow(id, price, stock, version);
                }
            }
        }
        pubKernel.storePubData(pubData);
    }

    private void loadPubData(IPubKernel pubKernel, String pubDataName) {
//		logger.info("begin loadPubData " + pubDataName);
        ICfgReader storeReader = pubKernel.loadXmlConfig("res/StoreItems/" + pubDataName + ".xml");
        int storeItemCount = storeReader.getItemCount();
        IPubData pubData = pubKernel.getPubData(pubDataName, true);
        IPubRecord record = pubData.getRecord("Record");
        if (record != null) {
            if (((STORE_EXCHANGE.equals(pubDataName) && record.getCols() != ITEM_EXCHANGE_COL.COL_END.ordinal())
                    || (STORE_EXCHANGE_CARD.equals(pubDataName)) && record.getCols() != ITEM_EXCHANGE_CARD_COL.COL_END.ordinal())
                    || (STORE_EXCHANGE_VIP_INTEGRAL.equals(pubDataName))
                    && record.getCols() != ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_END.ordinal()) {
                pubData.delRecord("Record");
            }
        }
        record = pubData.getRecord("Record");
        if (record == null) {
            if (STORE_EXCHANGE.equals(pubDataName)) {
                record = pubData.addRecord("Record", ITEM_EXCHANGE_COL.COL_END.ordinal(), 50, true);
                record.setColType(ITEM_EXCHANGE_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_COL.COL_SALE_STATUS.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_COL.COL_STOCK.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_COL.COL_VIRTUAL.ordinal(), ValueType.BOOL);
                record.setColType(ITEM_EXCHANGE_COL.COL_ITEM_NAME.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_COST_MATERIAL.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_PROPERTIES.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_VERSION.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_LIMIT.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_COL.COL_RESET.ordinal(), ValueType.BOOL);
                record.setColType(ITEM_EXCHANGE_COL.COL_COST_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_COL.COL_SWITCH.ordinal(), ValueType.BOOL);//开关
                record.setColType(ITEM_EXCHANGE_COL.COL_SEQUENCE.ordinal(), ValueType.INT);//排序值
            } else if (STORE_BATTERY.equals(pubDataName)) {
                record = pubData.addRecord("Record", ITEM_BATTERY_COL.COL_END.ordinal(), 50, true);
                record.setColType(ITEM_BATTERY_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_BATTERY_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_BATTERY_COL.COL_SALE_STATUS.ordinal(), ValueType.INT);
                record.setColType(ITEM_BATTERY_COL.COL_PROPERTIES.ordinal(), ValueType.STRING);
                record.setColType(ITEM_BATTERY_COL.COL_SHOW.ordinal(), ValueType.BOOL);
            } else if (STORE_EXCHANGE_CARD.equals(pubDataName)) {
                record = pubData.addRecord("Record", ITEM_EXCHANGE_CARD_COL.COL_END.ordinal(), 50, true);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_LIMIT.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_VIRTUAL.ordinal(), ValueType.BOOL);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_ITEM_NAME.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_COST_MATERIAL.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_PROPERTIES.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_SALES_COUNT.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_CARD_COL.COL_STOCK.ordinal(), ValueType.INT);
            } else if (STORE_EXCHANGE_VIP_INTEGRAL.equals(pubDataName)) {
                record = pubData.addRecord("Record", ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_END.ordinal(), 50, true);
                // COL_ITEM_ID, COL_PRICE, COL_SALES_COUNT, COL_STOCK,
                // COL_VIRTUAL, COL_ITEM_NAME, COL_COST_MATERIAL, COL_VERSION,
                // COL_CONDITION, COL_WEEK_RESET,COL_COST_ITEM_ID,
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_STOCK.ordinal(), ValueType.INT);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VIRTUAL.ordinal(), ValueType.BOOL);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_NAME.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_COST_MATERIAL.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VERSION.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_CONDITION.ordinal(), ValueType.STRING);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_WEEK_RESET.ordinal(), ValueType.BOOL);
                record.setColType(ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_COST_ITEM_ID.ordinal(), ValueType.STRING);
            } else {
                record = pubData.addRecord("Record", ITEM_COL.COL_END.ordinal(), 50, true);
                record.setColType(ITEM_COL.COL_ITEM_ID.ordinal(), ValueType.STRING);
                record.setColType(ITEM_COL.COL_PRICE.ordinal(), ValueType.INT);
                record.setColType(ITEM_COL.COL_SALE_STATUS.ordinal(), ValueType.INT);
            }

            for (int i = 0; i < storeItemCount; i++) {
                String id = storeReader.getString(i, "Id");
                int price = storeReader.getInt(i, "Price");
                int stock = storeReader.getInt(i, "Stock");
                boolean isVirtual = storeReader.getBool(i, "Virtual");
                String itemName = storeReader.getString(i, "ItemName");
                String costMaterial = storeReader.getString(i, "CostMaterial");
                String properties = storeReader.getString(i, "Properties");
                String version = storeReader.getString(i, PLAYER_PROPERTY_VERSION);
                String itemId = storeReader.getString(i, "ItemId");
                boolean show = storeReader.getBool(i, "Show");
                if (STORE_EXCHANGE.equals(pubDataName)) {
                    record.addRow(id, price, SaleStaus.NEW.ordinal(), 0, stock, isVirtual, itemName, costMaterial,
                            properties, version, storeReader.getInt(i, "Limit"), storeReader.getBool(i, "DayReset"),
                            itemId, storeReader.getBool(i, "Switch"), storeReader.getInt(i, "Sequence"));// 物品ID，价格，促销标识,已销售数量，库存,是否为虚拟物品,物品名称，消耗材料,消耗属性，版本
                    logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0,0," + stock + "," + isVirtual + "," + itemName + "," + costMaterial + "," + properties + "," + version);
                } else if (STORE_EXCHANGE_CARD.equals(pubDataName)) {
                    record.addRow(id, price, storeReader.getInt(i, "Limit"), isVirtual, itemName, costMaterial, properties, 0, stock);
                    logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0,0," + stock + "," + isVirtual + "," + itemName + "," + costMaterial + "," + properties + "," + version);
                } else if (STORE_BATTERY.equals(pubDataName)) {
                    record.addRow(id, price, SaleStaus.NEW.ordinal(), properties, show);// 物品ID，价格，促销标识,消耗属性,是否展现
                    logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0," + properties + "," + show);
                } else if (STORE_EXCHANGE_VIP_INTEGRAL.equals(pubDataName)) {
                    String condition = storeReader.getString(i, "Condition");
                    boolean weekReset = storeReader.getBool(i, "WeekReset");
                    Object[] objects = {id, price, 0, stock, isVirtual, itemName, costMaterial, version, condition, weekReset, itemId};
                    record.addRow(objects);
                    logger.info("add pubdata {}", Arrays.toString(objects));
                } else {
                    record.addRow(id, price, SaleStaus.NEW.ordinal());// 物品ID，价格，促销标识
                    logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0");
                }
            }
        } else {
            if (STORE_BATTERY.equals(pubDataName)) {
                record.clear();
            }
            // 查找是否有删除的物品
            for (int i = 0; i < record.getRows(); i++) {
                String itemId = record.getString(i, 0);
                int configRow = hasItemId(storeReader, itemId);
                if (-1 == configRow) {// 如果配置文件中没有此物品
                    logger.info("remove pubdata " + pubDataName + " Record:" + itemId + "," + record.getInt(i, 1) + "," + record.getInt(i, 2));
                    record.removeRow(i);
                    i--;
                } else {// 如果配置中有此物品
                    if (STORE_EXCHANGE.equals(pubDataName)) {// 且是兑换物品
                        String dbVersion = record.getString(i, ITEM_EXCHANGE_COL.COL_VERSION.ordinal());
                        String configVersion = storeReader.getString(configRow, PLAYER_PROPERTY_VERSION);
                        if (!dbVersion.equals(configVersion)) {// 如果配置版本变化,则重新加载库存等
                            logger.info("StoreExchange update");
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_VERSION.ordinal(), configVersion);
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal(), 0);
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_PRICE.ordinal(), storeReader.getInt(configRow, "Price"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_VIRTUAL.ordinal(), storeReader.getBool(configRow, "Virtual"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_ITEM_NAME.ordinal(), storeReader.getString(configRow, "ItemName"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_COST_MATERIAL.ordinal(), storeReader.getString(configRow, "CostMaterial"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_PROPERTIES.ordinal(), storeReader.getString(configRow, "Properties"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_LIMIT.ordinal(), -1); // 限制次数有Game服自行解析配置
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_RESET.ordinal(), storeReader.getBool(configRow, "DayReset"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_COST_ITEM_ID.ordinal(), storeReader.getString(configRow, "ItemId"));
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_SWITCH.ordinal(), storeReader.getBool(configRow, "Switch"));//开关
                            record.setValue(i, ITEM_EXCHANGE_COL.COL_SEQUENCE.ordinal(), storeReader.getInt(configRow, "Sequence"));//排序值
                        }
                    } else if (STORE_EXCHANGE_VIP_INTEGRAL.equals(pubDataName)) {
                        String dbVersion = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VERSION.ordinal());
                        String configVersion = storeReader.getString(configRow,
                                PLAYER_PROPERTY_VERSION);
                        if (!dbVersion.equals(configVersion)) {// 如果配置版本变化,则重新加载库存等
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_PRICE.ordinal(), storeReader.getInt(configRow, "Price"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal(), 0);
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_STOCK.ordinal(), storeReader.getInt(configRow, "Stock"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VIRTUAL.ordinal(), storeReader.getBool(configRow, "Virtual"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_ITEM_NAME.ordinal(), storeReader.getString(configRow, "ItemName"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_COST_MATERIAL.ordinal(), storeReader.getString(configRow, "CostMaterial"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_VERSION.ordinal(), configVersion);
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_CONDITION.ordinal(), storeReader.getString(configRow, "Condition"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_WEEK_RESET.ordinal(), storeReader.getBool(configRow, "WeekReset"));
                            record.setValue(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_COST_ITEM_ID.ordinal(), storeReader.getString(configRow, "ItemId"));
                        }
                    }
                }
            }
            // 查找是否有新增的物品
            for (int i = 0; i < storeItemCount; i++) {
                int row = record.findRow(0, 0, storeReader.getString(i, "Id"));
                if (-1 == row) {
                    String id = storeReader.getString(i, "Id");
                    int price = storeReader.getInt(i, "Price");
                    int stock = storeReader.getInt(i, "Stock");
                    boolean isVirtual = storeReader.getBool(i, "Virtual");
                    String itemName = storeReader.getString(i, "ItemName");
                    String costMaterial = storeReader.getString(i, "CostMaterial");
                    String properties = storeReader.getString(i, "Properties");
                    String itemId = storeReader.getString(i, "ItemId");
                    String version = storeReader.getString(i, PLAYER_PROPERTY_VERSION);
                    boolean show = storeReader.getBool(i, "Show");
                    if (STORE_EXCHANGE.equals(pubDataName)) {
                        record.addRow(id, price, SaleStaus.NEW.ordinal(), 0, stock, isVirtual, itemName, costMaterial,
                                properties, version, storeReader.getInt(i, "Limit"), storeReader.getBool(i, "DayReset"),
                                itemId, storeReader.getBool(i, "Switch"), storeReader.getInt(i, "Sequence"));// 物品ID，价格，促销标识,已销售数量，库存,是否为虚拟物品,物品名称，消耗材料,消耗属性，版本
                        //logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0,0," + stock + ","+ isVirtual + "," + itemName + "," + costMaterial + "," + properties + "," + version);
                    } else if (STORE_EXCHANGE_CARD.equals(pubDataName)) {
                        record.addRow(id, price, storeReader.getInt(i, "Limit"), isVirtual, itemName, costMaterial, properties, 0, stock);
                        //logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0,0," + stock + ","+ isVirtual + "," + itemName + "," + costMaterial + "," + properties + "," + version);
                    } else if (STORE_BATTERY.equals(pubDataName)) {
                        record.addRow(id, price, SaleStaus.NEW.ordinal(), properties, show);// 物品ID，价格，促销标识,消耗属性,是否展现
                        //logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0," + properties+ "," + show);
                    } else if (STORE_EXCHANGE_VIP_INTEGRAL.equals(pubDataName)) {
                        Object[] objects = {id, price, 0, stock, isVirtual, itemName, costMaterial, version, storeReader.getString(i, "Condition"), storeReader.getBool(i, "WeekReset"), itemId};
                        record.addRow(objects);
                        //logger.info("add pubdata {}", Arrays.toString(objects));
                    } else {
                        record.addRow(id, price, SaleStaus.NEW.ordinal());// 物品ID，价格，促销标识
                        //logger.info("add pubdata " + pubDataName + " Record:" + id + "," + price + ",0");
                    }
                }
            }
        }
        pubKernel.storePubData(pubData);
    }

    void OnUpdateExchange(IPubKernel pubKernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        ServerMsg.UpdateStoreExchange msg = ServerMsg.UpdateStoreExchange.parseFrom(data);
        String item = msg.getItem();
        int stock = msg.getStock();
        int limit = msg.getLimit();
        boolean reset = msg.getReset();
        IPubData pubData = pubKernel.getPubData("StoreExchange", false);
        IPubRecord record = pubData.getRecord("Record");
        int pos = record.findRow(0, ITEM_EXCHANGE_COL.COL_ITEM_ID.ordinal(), item);
        if (pos != -1) {
            record.setValue(pos, ITEM_EXCHANGE_COL.COL_STOCK.ordinal(), stock);
            record.setValue(pos, ITEM_EXCHANGE_COL.COL_LIMIT.ordinal(), limit);
            record.setValue(pos, ITEM_EXCHANGE_COL.COL_RESET.ordinal(), reset);
            pubKernel.storePubData(pubData);
        }
        pubKernel.responseServer(reqid, null);
    }


    void OnUpdateStore(IPubKernel pubKernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        String name = new String(data);
        if (name.equals("")) {
            pubKernel.responseServer(reqid, new byte[]{0});
            return;
        }
        switch (name) {
            case STORE_DIAMOND:
                loadPubData(pubKernel, STORE_DIAMOND);
                break;
            case STORE_DIAMOND + "Ios":
                loadPubData(pubKernel, STORE_DIAMOND + "Ios");
                break;
            case STORE_GOLD:
                loadPubData(pubKernel, STORE_GOLD);
                break;
            case STORE_GOLD + "Ios":
                loadPubData(pubKernel, STORE_GOLD + "Ios");
                break;
            case STORE_EXCHANGE:
                loadPubData(pubKernel, STORE_EXCHANGE);
                break;
            case STORE_BATTERY:
                loadPubData(pubKernel, STORE_BATTERY);
                break;
            case STORE_EXCHANGE_CARD:
                loadPubData(pubKernel, STORE_EXCHANGE_CARD);
                break;
            case STORE_EXCHANGE_VIP_INTEGRAL:
                loadPubData(pubKernel, STORE_EXCHANGE_VIP_INTEGRAL);
                break;
            default:
                pubKernel.responseServer(reqid, new byte[]{0});
                return;

        }
        pubKernel.responseServer(reqid, new byte[]{1});
    }

    private int hasItemId(ICfgReader reader, String itemId) {
        int storeRechargeItemCount = reader.getItemCount();
        for (int i = 0; i < storeRechargeItemCount; i++) {
            if (itemId.equals(reader.getString(i, "Id"))) {
                return i;
            }
        }
        return -1;
    }
}
