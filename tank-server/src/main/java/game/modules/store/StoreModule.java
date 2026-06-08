package game.modules.store;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.JsonUtil;
import framework.SystemConfigData;
import framework.game.*;
import framework.game.IKernel.PlayLogType;
import framework.mybatis.domain.PayChannel;
import framework.mybatis.service.impl.PayChannelService;
import framework.net.HttpClientUtil;
import framework.pub.IPubData;
import framework.pub.IPubRecord;
import game.constant.OfflineDataType;
import game.custommsg.*;
import game.modules.OfflineDataModule;
import game.modules.TimerManager;
import game.modules.fishgame.BulletValModule;
import game.modules.items.ItemModule;
import game.modules.player.PlayerModule;
import game.modules.player.PlayerModule.OptType;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import game.util.XML;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商城
 *
 * @author MaDi
 *
 */
public class StoreModule implements ILogicModule {

	private static Logger logger = LoggerFactory.getLogger(StoreModule.class);

	public static final int SPECIAL_GOODS_BUY_VALID_TIME = 2 * 60 * 1000;

	private static String PAY_POP_TIME_STR = "loginPopPaySuccMsg";
	Random mRand = new Random();
	/**
	 * 兑换商城兑换前首次输入的手机号
	 */
	public static final String PROPERTY_FIRST_TEMP_CELLPHONE = PLAYER_PROPERTY_FIRSTTEMPCELLPHONE;
	/**
	 * 兑换商城兑换前最后输入的手机号
	 */
	public static final String PROPERTY_LAST_TEMP_CELLPHONE = PLAYER_PROPERTY_LASTTEMPCELLPHONE;
	public static final String PROPERTY_LAST_TIME_GET_COUPONS = "lastTimeGetCoupons";
	public static final String PROPERTY_DAILY_COUPONS_STATE= "dailyCouponsState";

	private TimerManager timerManager;

	private String[] pubDataNames = { "StoreDiamond", "StoreGold", "StoreBattery", "StoreExchange", "StoreExchangeCard",
			"StoreExchangeVipIntegral","","StoreCoupons"};

	private String friendInvitation = "item_friendinvitation";

	private Map<String, Integer> payItems = new HashMap<>();

	private List<String> noDispatchItems = new ArrayList<>();
	// private Map<String, Integer> paySpecialItems = new HashMap<String,
	// Integer>();

	private Map<String, Integer> firstDoubleItems = new HashMap<>();

	// ================= Pay/Order 幂等 & 状态机 =================
	// 防止支付回调重复触发导致的重复发放（以 orderId 为幂等键）
	private static final String PAY_ORDER_REC = "PayOrderRec";
	// 1: 已支付（已校验回调参数，等待发货）
	// 2: 已发货（金币/钻石/优惠等已完成发放，后续回调忽略）
	private static final int PAY_ORDER_STATUS_PAID = 1;
	private static final int PAY_ORDER_STATUS_SHIPPED = 2;
	// 保留时间：避免 orderId 在一段时间后再次回调造成重复奖励
	private static final long PAY_ORDER_REC_TTL_MS = 24L * 60 * 60 * 1000;

	private Map<Integer, Integer> channelIntegral = new HashMap<>();
	private Map<Integer, Float> channelColorTicketDropRatio = new HashMap<>();
	private Map<Integer, Boolean> channelCertification = new HashMap<>();
	private Map<Integer, Integer> channelPubRatio = new HashMap<>();
	private Map<Integer, Long> channelCurPubSocre = new HashMap<>();
	private Map<Integer, Long> channelMaxPubSocre = new HashMap<>();

	private Map<String, Integer> canBuy4TestItems = new HashMap<>();

	private Map<String, Integer> canBuy4Coupons = new HashMap<>();
	private Map<Integer, String> vipFreeAward = new HashMap<>();

	// 兑换限制
	private Map<String, int[]> mapExchangeLimit = new HashMap<>();
	private Map<String, Integer> mapExchangeCardLimit = new HashMap<>();

	class OrderResTip {
		ItemTipType type;
		int orderId;
		Object[] objects;
	}

	// 下单时间限制
	class PerOrderLimit {
		int uid;		// 玩家uid
		long perTime;	// 上次下单时间
		int counts;		// 下单次数
	}

	Map<Integer,PerOrderLimit> m_listPerOrderLimit = new HashMap<>();

	Map<Integer, OrderResTip> m_mapOrderRes = new HashMap<>(); // 订单购买结果
	boolean m_activePush = false;

	enum ITEM_COL {
		COL_ITEM_ID, COL_PRICE, COL_SALE_STATUS, COL_END
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

	enum ITEM_EXCHANGE_VIP_INTEGRAL_COL {
		COL_ITEM_ID, COL_PRICE, COL_SALES_COUNT, COL_STOCK, COL_VIRTUAL, COL_ITEM_NAME, COL_COST_MATERIAL, COL_VERSION, COL_CONDITION, COL_WEEK_RESET, COL_COST_ITEM_ID,

		COL_END
	}

	enum ITEM_TYPE {
		DIAMOND, GOLD, GUN_SKIN, EXCHANGE, EXCHANGE_CARD, EXCHANGE_VIP_INTEGRAL,UNKOWN,COUPONS
	}

	private ItemModule m_itemModule = null;
	private PlayerModule m_PlayerModule = null;
	private OfflineDataModule offlineDataModule = null;
	BulletValModule bulletValModule = null;
	private XML m_parseXML;

	void RefreshCfg(IKernel kernel, String path) {
		if (path == null) {
			return;
		}
		m_parseXML.parse(kernel, path);
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		// 定义接收的用户消息
		kernel.regCommand(CommandDef.CMD_CHANGE_DAY.ordinal(), "Player", this, "OnChangeDay");
		kernel.regClientMessage(C2SMsgDef.C2S_GET_STORE_ITEMS.ordinal(), this, "OnGetStoreItems");
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_PAY.ordinal(), this, "OnBuyStorePay");// 商城购买钻石或金币
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_ITEM.ordinal(), this, "OnBuyStoreItem");// 商城钻石购买物品
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_GOODS.ordinal(), this, "OnBuyStoreGoods");// 商城兑换物品
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_PAY_BY_COUPONS.ordinal(), this, "OnBuyStoreGoodsByCoupons");//new   商城点券购买物品
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_PAY_FOR_COUPONS.ordinal(), this, "OnBuyCoupons");//new   商城点券购买物品
		kernel.regClientMessage(C2SMsgDef.C2S_BUY_STORE_FREE_COUPONS.ordinal(), this, "OnGetFreeCoupons");//new   商城点券购买物品

		//kernel.RegClientMessage(C2SMsgDef.C2S_PAY_RESULT.ordinal(), this, "OnPayResult");
		// H2G_PAY_BACK 支付回调：迁移到 MallsModule（新逻辑包含幂等/状态机/金额封顶）
		// kernel.regServerRequest(ServerMsgDef.H2G_PAY_BACK.ordinal(), this, "OnRecPayCallBack");
		kernel.regServerRequest(ServerMsgDef.H2G_CHANNEL_DATA_CHANGE.ordinal(), this, "OnRecChannelDataChannel");
		kernel.regServerRequest(ServerMsgDef.B2G_GET_CHANNEL_COLOR_TICKET_DROP_RATIO.ordinal(), this, "OnGetChannelColorTicketDropRadio");
		//kernel.RegServerMsg(ServerMsgDef.H2G_PAY_BACK.ordinal(), this, "OnPayCallBack");
		kernel.regServerMsg(ServerMsgDef.MASTER_ACTIVE_PUSH.ordinal(), this, "OnActivePush");
		// REQ_ORDER_RES 订单结果请求：迁移到 MallsModule（本实现直接发放，返回空结构即可）
		// kernel.regRequestMessage(RequestMsgDef.REQ_ORDER_RES.ordinal(), this, "OnReqOrderInfo"); // 请求订单信息
		kernel.regRequestMessage(RequestMsgDef.REQ_SET_TEMP_CELLPHONE.ordinal(), this, "OnReqSetTempCellphone");

		// kernel.DeclareHeartBeat("HB_CheckOrder", this, "OnCheckOrder");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");

		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.declareHeartBeat(PAY_POP_TIME_STR, this, "OnTimeEnd");

		kernel.listenPropertyChange(PLAYER_PROPERTY_VIPLEVEL, "Player", this, "OnVipLevelChanged");
		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		m_PlayerModule = (PlayerModule) kernel.getModule("PlayerModule");
		offlineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");
		bulletValModule = (BulletValModule) kernel.getModule("BulletValModule");


		m_activePush = SystemConfigData.getConfig("activePush", false);

		timerManager = (TimerManager) kernel.getModule("TimerManager");
		if (null == timerManager) {
			logger.error("Error,TimerManager is null");
			return false;
		}
		timerManager.addChangeDayCallBack(this, "OnChangeDay");
		timerManager.addChangeWeekCallBack(this, "OnChangeWeek");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		XML storeDiamond = new XML("res/StoreItems/StoreDiamond.xml", null, kernel, (iKernel, cfg) -> loadDiamondAndGold(iKernel, cfg));
		XML storeDiamondIos = new XML("res/StoreItems/StoreDiamondIos.xml", storeDiamond, kernel, (iKernel, cfg) -> loadDiamondAndGold(iKernel, cfg));
		XML storeGold = new XML("res/StoreItems/StoreGold.xml", storeDiamondIos, kernel, (iKernel, cfg) -> loadDiamondAndGold(iKernel, cfg));
		XML storeGoldIos = new XML("res/StoreItems/StoreGoldIos.xml", storeGold, kernel, (iKernel, cfg) -> loadDiamondAndGold(iKernel, cfg));

		XML storePkg = new XML("res/StoreItems/StorePkg.xml", storeGoldIos, kernel, (iKernel, cfg) -> loadPkg(iKernel, cfg));
		//XML preOrderUrl = new XML("res/PreOrderUrl/PreOrderUrl.xml", storePkg, kernel,(iKernel, cfg) -> loadPreOrderUrl(iKernel, cfg));
		XML storeTest = new XML("res/StoreItems/StoreTest.xml", storePkg, kernel, (iKernel, cfg) -> loadTest(iKernel, cfg));
		XML VIPFree = new XML("res/StoreItems/VIPFreeCoupons.xml", storeTest, kernel, (iKernel, cfg) -> loadVIPFree(iKernel, cfg));
		XML storeCoupons = new XML("res/StoreItems/StoreCoupons.xml", VIPFree, kernel, (iKernel, cfg) -> loadCoupons(iKernel, cfg));
		XML storeCouponsIos = new XML("res/StoreItems/StoreCouponsIos.xml", storeCoupons, kernel, (iKernel, cfg) -> loadCoupons(iKernel, cfg));
		XML storeExchange = new XML("res/StoreItems/StoreExchange.xml", storeCouponsIos, kernel, (iKernel, cfg) -> loadExchange(iKernel, cfg));
		m_parseXML = new XML("res/StoreItems/StoreExchangeCard.xml", storeExchange, kernel, (iKernel, cfg) -> loadExchangeCard(iKernel, cfg));
		XML directPurchaseGiftPack = new XML("res/StoreItems/DirectPurchaseGiftPack.xml", null, kernel, (iKernel, cfg) -> loadDiamondAndGold(iKernel, cfg));

		return true;
	}

	@Override
	public void onNetReady(IKernel kernel) {
		kernel.executeSomeToStore(PayChannelService.class,"loadAll",null,(str)->{
			if (str == null){
				return;
			}
			List<PayChannel> channels = JsonUtil.decodeToList(str,PayChannel.class);
			channelIntegral.clear();
			channelColorTicketDropRatio.clear();
			channelCertification.clear();
			channelPubRatio.clear();
			if (channels != null && channels.size() > 0){
				channels.forEach(channel->{
					Integer channelId = channel.getId();
					channelCurPubSocre.put(channelId,channel.getCurPubScore());
					channelMaxPubSocre.put(channelId,channel.getMaxPubScore());
					channelChange(false,channelId,channel.getItemScore(),channel.getDropLotteryRatio() * 1.0f,channel.getCertification(),channel.getPubRatio());
				});
			}
		});
	}

	void channelChange(boolean delete,Integer channelId,Integer itemScore,Float dropRatio,int flag,int pubRatio){
		if (delete){
			channelIntegral.remove(channelId);
			channelColorTicketDropRatio.remove(channelId);
			channelCertification.remove(channelId);
			channelPubRatio.remove(channelId);
		}else{
			channelIntegral.put(channelId,itemScore);
			channelColorTicketDropRatio.put(channelId,dropRatio);
			channelCertification.put(channelId,flag == 1);
			channelPubRatio.put(channelId,pubRatio);
		}
	}

	void OnTimeEnd(IKernel kernel, IGameObject player) {
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		if (m_mapOrderRes.containsKey(uid)){
			OrderResTip orderTip = m_mapOrderRes.remove(uid);
			UtilFunc.sendItemTips(kernel,player,orderTip.type,orderTip.objects);
		}
		kernel.removeHeartBeat(player,PAY_POP_TIME_STR);
	}

	void OnPlayerOnLine(IKernel kernel, IGameObject player){
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		boolean sameDay = UtilFunc.isSameDay(player.getLong(PROPERTY_LAST_TIME_GET_COUPONS), kernel.getServerTime());
		if(!sameDay){
			player.setProperty(PROPERTY_LAST_TIME_GET_COUPONS,kernel.getServerTime());
			player.setProperty(PROPERTY_DAILY_COUPONS_STATE,false);
			logger.info("player:{} sameDay={} 重置商城免费领取金币状态:{}",player.getProperty("Uid"),sameDay,player.getProperty(PROPERTY_DAILY_COUPONS_STATE));
		}
		if (m_mapOrderRes.containsKey(uid)){
			//有需要弹出的支付成功的弹框
			logger.info("{} had pay msg pop", uid);
			kernel.addHeartBeat(PAY_POP_TIME_STR,player,4,1);
		}
	}

	void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PROPERTY_LAST_TIME_GET_COUPONS, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, PROPERTY_DAILY_COUPONS_STATE, ValueType.BOOL, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, ValueType.INT, false, true, true);
		kernel.declareProperty(script, BombValueTotal, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, BombMiniGameValueTotal, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, BombShzGameValueTotal, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALRECHARGEAMOUNT_COUNPONS, ValueType.INT, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_ALREADYRECHARGED, ValueType.BOOL, false, true, true);
		kernel.declareProperty(script, PROPERTY_FIRST_TEMP_CELLPHONE, ValueType.STRING, false, true, true);
		kernel.declareProperty(script, PROPERTY_LAST_TEMP_CELLPHONE, ValueType.STRING, false, true, true);

		IRecord recExchangeLimit = kernel.declareRecord(script, "ExchangeLimit", 3, 100, false, true, true);
		recExchangeLimit.setColType(0, ValueType.STRING); // 兑换道具ID
		recExchangeLimit.setColType(1, ValueType.LONG); // 限制版本
		recExchangeLimit.setColType(2, ValueType.INT); // 剩余购买次数

		IRecord recExchangeCardLimit = kernel.declareRecord(script, "ExchangeCardLimit", 3, 100, false, true, true);
		recExchangeCardLimit.setColType(0, ValueType.STRING); // 兑换道具ID
		recExchangeCardLimit.setColType(1, ValueType.LONG); // 限制版本
		recExchangeCardLimit.setColType(2, ValueType.INT); // 剩余购买次数

		IRecord buyGoodsRec = kernel.declareRecord(script, "BuyGoodsRec", 4, 100, false, false, true);
		buyGoodsRec.setColType(0, ValueType.STRING);// 购买物品id
		buyGoodsRec.setColType(1, ValueType.LONG);// 购买时间
		buyGoodsRec.setColType(2, ValueType.INT);// 内部订单号
		buyGoodsRec.setColType(3, ValueType.STRING);// 附加字段

		// 支付回调幂等/状态机：orderId -> {status, goodsId}
		IRecord payOrderRec = kernel.declareRecord(script, PAY_ORDER_REC, 4, 200, false, false, true);
		payOrderRec.setColType(0, ValueType.INT); // orderId
		payOrderRec.setColType(1, ValueType.INT); // status
		payOrderRec.setColType(2, ValueType.STRING); // goodsId
		payOrderRec.setColType(3, ValueType.LONG); // updateTime

		IRecord alreadyBuyRec = kernel.declareRecord(script, "AlreadyBuyRec", 1, 50, false, true, true);
		alreadyBuyRec.setColType(0, ValueType.STRING);// 已经购买过的物品

		IRecord testAlreadyBuyRec = kernel.declareRecord(script, "TestAlreadyBuyRec", 1, 20, false, true, true);
		testAlreadyBuyRec.setColType(0, ValueType.STRING);// 已经购买过的物品

	}

	// VIP等级变化时，更新限购次数 add by 胡中伟, 2019年4月11日 下午2:02:50
	void OnVipLevelChanged(IKernel kernel, IGameObject player, String name, Object oldParam) {
		long now = UtilFunc.getZeroTime(kernel.getServerTime());
		IRecord rec = player.getRecord("ExchangeLimit");
		for (int i = 0; i < rec.getRows(); ++i) {
			String item = rec.getString(i, 0);
			int limit = GetLimit(player, item);
			if (rec.getLong(i, 1) != now) {
				rec.setValue(i, 1, now);
				rec.setValue(i, 2, limit);
			} else {
				int oldLimit = GetLimit((int) oldParam, item);
				int newVal = rec.getInt(i, 2) + limit - oldLimit;
				if (newVal < 0) {
					newVal = 0;
				}
				rec.setValue(i, 2, newVal);
			}
		}
	}

	int GetLimit(int vipLevel, String item) {
		if (!mapExchangeLimit.containsKey(item)) {
			return -1;
		}
		int[] limits = mapExchangeLimit.get(item);
		if (vipLevel >= limits.length) {
			return limits[limits.length - 1];
		}
		return limits[vipLevel];
	}

	int GetLimit(IGameObject player, String item) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		return GetLimit(vipLevel,item);
	}

	int GetVipLockLevel(String item) {
		int[] limits = mapExchangeLimit.get(item);
		if (limits != null){
			for (int i = 0; i < limits.length; i++) {
				if (limits[i] != 0) {
					return i;
				}
			}
		}
		return 0;
	}

	int GetCardLimit(String item) {
		if (!mapExchangeCardLimit.containsKey(item)) {
			return -1;
		}
		return mapExchangeCardLimit.get(item);
	}

	boolean CheckCardLimit(IKernel kernel, IGameObject player, String item, int count) {
		int limit = GetCardLimit(item);
		if (limit == -1) {
			return true;
		}

		long now = kernel.getServerTime();

		IRecord rec = player.getRecord("ExchangeCardLimit");
		int row = rec.findRow(0, 0, item);
		if (row == -1) {
			row = rec.getRows();
			rec.addRow(item, now, limit);
		}

		Calendar lastVersion = Calendar.getInstance();
		lastVersion.setTimeInMillis(rec.getLong(row, 1));
		Calendar thisVersion = Calendar.getInstance();
		thisVersion.setTimeInMillis(now);
		if (lastVersion.get(Calendar.WEEK_OF_YEAR) != thisVersion.get(Calendar.WEEK_OF_YEAR)) {
			rec.setValue(row, 1, now);
			rec.setValue(row, 2, limit);
		}

		int have = rec.getInt(row, 2);
		if (have < count) {
			logger.info("have < coun {} {}", have, count);
			return false;
		}

		rec.setValue(row, 2, have - count);

		return true;
	}

	public void BackCardLimitCount(IKernel kernel, IGameObject player, String item, int count) {
		int limit = GetLimit(player, item);
		long now = UtilFunc.getZeroTime(kernel.getServerTime());

		IRecord rec = player.getRecord("ExchangeCardLimit");
		int row = rec.findRow(0, 0, item);
		if (row == -1) {
			return;
		}

		if (rec.getLong(row, 1) != now) {
			rec.setValue(row, 1, now);
			rec.setValue(row, 2, limit);
		} else {
			int have = rec.getInt(row, 2) + count;
			if (have > limit) {
				have = limit;
			}
			rec.setValue(row, 2, have);
		}
	}

	int CheckVipLimit(IGameObject player, String item) {
		int lockLevel = GetVipLockLevel(item);
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (vipLevel < lockLevel) {
			return lockLevel;
		}
		return 0;
	}

	boolean CheckLimit(IKernel kernel, IGameObject player, String item, int count) {
		int limit = GetLimit(player, item);
		if (limit == -1) {
			return true;
		}

		long now = UtilFunc.getZeroTime(kernel.getServerTime());

		IRecord rec = player.getRecord("ExchangeLimit");
		int row = rec.findRow(0, 0, item);
		if (row == -1) {
			row = rec.getRows();
			rec.addRow(item, now, limit);
		}

		if (rec.getLong(row, 1) != now) {
			rec.setValue(row, 1, now);
			rec.setValue(row, 2, limit);
		}

		int have = rec.getInt(row, 2);
		if (have < count) {
			logger.info("have < coun {} {}", have, count);
			return false;
		}

		rec.setValue(row, 2, have - count);

		return true;

	}

	public void BackLimitCount(IKernel kernel, IGameObject player, String item, int count) {
		int limit = GetLimit(player, item);
		long now = UtilFunc.getZeroTime(kernel.getServerTime());

		IRecord rec = player.getRecord("ExchangeLimit");
		int row = rec.findRow(0, 0, item);
		if (row == -1) {
			return;
		}

		if (rec.getLong(row, 1) != now) {
			rec.setValue(row, 1, now);
			rec.setValue(row, 2, limit);
		} else {
			int have = rec.getInt(row, 2) + count;
			if (have > limit) {
				have = limit;
			}
			rec.setValue(row, 2, have);
		}
	}

	void OnChangeDay(IKernel kernel, int day) {
		if (kernel.isMain()) {
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_ON_CHANGE_DAY.ordinal(), new byte[] {});
		}
	}
	void OnChangeDay(IKernel kernel, IGameObject player) {
		player.setProperty(PROPERTY_DAILY_COUPONS_STATE,false);
		logger.info("player:{} 重置商城免费领取金币状态:{}",player.getProperty("Uid"),player.getProperty(PROPERTY_DAILY_COUPONS_STATE));

	}
	void OnChangeWeek(IKernel kernel, int day) {
		if (kernel.isMain()) {
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_ON_CHANGE_WEEK.ordinal(), new byte[] {});
		}
	}

	@Override
	public void onDestroy() {

	}

	public void OnItemClassCreate(IKernel kernel, String script) {

	}

	void OnRecChannelDataChannel(IKernel kernel, int reqid, byte[] data){
		byte[] resp = null;
		try {
			String str = new String(data,StandardCharsets.UTF_8);
			logger.info("OnRecChannelDataChannel {} ",str);
			JsonObject json = JsonUtil.decodeToObj(str,JsonObject.class);
			int type = json.get("t").getAsInt();
			JsonObject channel = json.get("d").getAsJsonObject();
			Integer channelId = channel.get("id").getAsInt();
			Integer itemScore = channel.get("itemScore").getAsInt();
			Integer dropLotteryRatio = channel.get("dropLotteryRatio").getAsInt();
			Integer certification    = channel.get("certification").getAsInt();
			Integer pubRatio = channel.get("pubRatio").getAsInt();
			channelChange(type == 1,channelId,itemScore,dropLotteryRatio * 1.0f,certification,pubRatio);
			resp = "ok".getBytes(StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.error("OnRecChannelDataChannel",e);
			resp = "fail".getBytes(StandardCharsets.UTF_8);
		}
		kernel.responseServer(reqid,resp);
	}

	void OnRecPayCallBack(IKernel kernel, int reqid, byte[] data) {
		byte[] result = new byte[1];
		try {
			ServerMsg.PayBack payBack = ServerMsg.PayBack.parseFrom(data);
			int playerId   = payBack.getUid();
			String goodsId = payBack.getGoodId();
			int payMoney   = Integer.parseInt(payBack.getPayMoney());
			String info    = payBack.getInfo();
			int orderId    = payBack.getOrderId();
			long orderSuccessTime = kernel.getServerTime();
			logger.info("player:" + playerId + "  OnRecPayCallBack ,goodsId:" + goodsId + ",info:" + info + ",orderSuccessTime:" + orderSuccessTime);
			if (payItems.containsKey(goodsId)) {
				IGameObject player = kernel.getPlayer(playerId);
				if (null != player) {
					logger.info("player:" + playerId + " is online,start doPayLogic");
					doPayLogic(kernel, player, payBack, orderSuccessTime, false, orderId);
				} else {
					logger.info("player:" + playerId + " is offline,start AddOfflineData");
					StringBuilder sb = new StringBuilder();
					sb.append(goodsId).append("-").append(payMoney).append("-").append(info).append("-").append(orderSuccessTime).append("-").append(orderId).append("-").append(payBack.hasCashTicket() ? payBack.getCashTicket() : "");
					offlineDataModule.AddOfflineData(kernel, playerId, OfflineDataType.PAY_CALL_BACK, sb.toString(),"PayCallBack");
				}
				result[0] = 0;
			}else{
				logger.error("OnRecPayCallBack Error! does not have goodsId:" + goodsId);
				result[0] = 2;
			}
		} catch (Exception e) {
			result[0] = 1;
			logger.error("Excute OnRecPayCallBack Error ",e);
		}
		kernel.responseServer(reqid,result);
	}

	void OnGetChannelColorTicketDropRadio(IKernel kernel, int reqid, byte[] data) throws Exception {
		List<Integer> channelList = JsonUtil.decodeToList(new String(data), Integer.class);
		Map<Integer, Float> map = channelList.stream().collect(Collectors.toMap(Function.identity(), this::getChannelColorTicketDropRatio));
		kernel.responseServer(reqid, ByteUtils.objectToByte(map));
	}

	public void doPayLogic(IKernel kernel, IGameObject player, ServerMsg.PayBack payBack, long orderSuccessTime,boolean offline, int orderId) {
		// 委托到新实现：MallsModule 负责支付金额归一化/封顶、幂等状态机、现金券扣除等
		try {
			game.modules.weapon.MallsModule mallsModule = (game.modules.weapon.MallsModule) kernel.getModule("MallsModule");
			if (mallsModule != null) {
				mallsModule.doPayLogic(kernel, player, payBack, orderSuccessTime, offline, orderId);
				return;
			}
		} catch (Exception ignored) {
		}
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		long now = kernel.getServerTime();
		IRecord payOrderRec = player.getRecord(PAY_ORDER_REC);
		int payRow = payOrderRec != null ? payOrderRec.findRow(0, 0, orderId) : -1;

		// 幂等：已进入“已支付/已发货”则忽略（防止重复扣券/重复发放）
		if (payRow != -1) {
			int st = payOrderRec.getInt(payRow, 1);
			if (st >= PAY_ORDER_STATUS_SHIPPED) {
				logger.info("uid={} doPayLogic skip, orderId={} status={}", uid, orderId, st);
				return;
			}
		}

		String goodsId = payRow != -1 ? payOrderRec.getString(payRow, 2) : OnCheckOrder(kernel, player, orderId);
		String info = payBack.getInfo();
		if (goodsId == null) {
			if (info.equals("")) {
				goodsId = payBack.getGoodId();
			} else {
				logger.info("can not find order id = " + orderId);
				return;
			}
		}

		// 先写入“已支付”状态，锁定 orderId
		if (payRow == -1 && payOrderRec != null) {
			payOrderRec.addRow(orderId, PAY_ORDER_STATUS_PAID, goodsId, now);
			payRow = payOrderRec.findRow(0, 0, orderId);
		}

		int payMoney = Integer.parseInt(payBack.getPayMoney());

		// 金额归一化/校验：以商品配置 Cost 为准（同时按“上限”封顶）
		int expectedYuan = 0;
		try {
			String costStr = kernel.getCfgProperty(goodsId, "Cost");
			if (costStr != null && !costStr.isEmpty()) {
				expectedYuan = Integer.parseInt(costStr);
			}
		} catch (Exception ignored) {
		}
		if (expectedYuan > 0) {
			// 常见的“单位差异”适配：raw=expected*10
			if (payMoney == expectedYuan * 10) {
				payMoney = expectedYuan;
			}
			if (payMoney < expectedYuan) {
				logger.info("uid={} underpay reject: orderId={}, goodsId={}, payMoney={}, expectedYuan={}",
						uid, orderId, goodsId, payMoney, expectedYuan);
				if (payRow != -1 && payOrderRec != null) {
					payOrderRec.removeRow(payRow);
				}
				return;
			}
			// 过大按配置上限封顶
			if (payMoney > expectedYuan) {
				payMoney = expectedYuan;
			}
		}
		if (payBack.hasCashTicket()) {
			String cashTicket = payBack.getCashTicket();
			if (cashTicket != null && !"".equals(cashTicket)) {
				int count = m_itemModule.SubItem(kernel, player, cashTicket, 1, UtilFunc.System.STORE.ordinal(),"pay use cashTicket");
				if (count <= 0) {//不够扣，不给发货
					logger.info("uid={} lack cashTicket:{}", uid, cashTicket);
					if (payRow != -1 && payOrderRec != null) {
						// 还未真正发货：回滚幂等记录，允许后续回调重试
						int st = payOrderRec.getInt(payRow, 1);
						if (st < PAY_ORDER_STATUS_SHIPPED) {
							payOrderRec.removeRow(payRow);
						}
					}
					return;
				}
			}
		}
		IRecord alreadyBuyRec = player.getRecord("AlreadyBuyRec");
		boolean firstBuy = false;
		int row = alreadyBuyRec.findRow(0,0,goodsId);
		if (-1 == row) {
			alreadyBuyRec.addRow(goodsId);
			firstBuy = true;
		}
		// 记录玩家操作
		m_PlayerModule.SetLastOpt(player, OptType.TYPE_BUY_GOODS.ordinal(), goodsId);
		// 记录当前携带
		String orderLog = new StringBuilder().append("Gold:").append(player.getLong(PLAYER_PROPERTY_GOLD)).append(",Diamond:")
				.append(player.getLong(PLAYER_PROPERTY_DIAMOND)).append(",ColorTicket:").append(player.getLong(PLAYER_PROPERTY_COLORTICKET))
				.append(",item_powerstone:").append(m_itemModule.GetItemCount(kernel, player, "item_powerstone"))
				.append(",item_stone:").append(m_itemModule.GetItemCount(kernel, player, "item_stone"))
				.append(",item_mithril:").append(m_itemModule.GetItemCount(kernel, player, "item_mithril"))
				.append(",item_skill_bind_bomb:")
				.append(m_itemModule.GetItemCount(kernel, player, "item_skill_bind_bomb"))
				.append(",item_skill_normal_bomb:")
				.append(m_itemModule.GetItemCount(kernel, player, "item_skill_normal_bomb"))
				.append(",item_skill_missile:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_missile"))
				.append(",item_skill_nbomb:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_nbomb"))
				.append(",item_skill_hbomb:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_hbomb"))
				.toString();
		kernel.addPlayLog(player, null, PlayLogType.PAY_ORDER, orderLog, "Buy " + goodsId);
		if (!noDispatchItems.contains(goodsId)) {
			if (firstDoubleItems.containsKey(goodsId) && firstBuy) {// 首充翻倍物品
				m_itemModule.AddItem(kernel, player, goodsId, 2, UtilFunc.System.STORE.ordinal(), "first double"); // 物品加到背包里
				if (offline) {
					UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, goodsId, 2);
				} else {
					AddOrderResTip(kernel, player, orderId, ItemTipType.TIP_PAY, goodsId, 2);
				}
			} else {
				m_itemModule.AddItem(kernel, player, goodsId, 1, UtilFunc.System.STORE.ordinal(), "not first"); // 物品加到背包里
				if (offline) {
					UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, goodsId, 1);
				} else {
					AddOrderResTip(kernel,player,orderId, ItemTipType.TIP_PAY, goodsId, 1);
				}
			}
		}
		// 发货前锁定为“已发货”（幂等），避免重复回调导致重复扣券/重复发放
		if (payRow != -1 && payOrderRec != null) {
			payOrderRec.setValue(payRow, 1, PAY_ORDER_STATUS_SHIPPED);
			payOrderRec.setValue(payRow, 3, now);
		}

		removeRecordOrder(kernel,player,orderId);
		player.setProperty(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT) + payMoney);
		player.setProperty(PLAYER_PROPERTY_ALREADYRECHARGED, true);
		kernel.command(player, CommandDef.CMD_PAY_BACK.ordinal(), goodsId, payMoney, info, orderSuccessTime,offline,String.valueOf(orderId));
	}

	public void OnGetStoreItems(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.GetStoreItems getStoreItems = CustomMsg.GetStoreItems.parseFrom(data);
		int itemType = getStoreItems.getItemType();
		if (itemType < 0 || itemType >= pubDataNames.length) {
			logger.error("OnGetStoreItems error itemType=" + itemType);
			return;
		}
		String pubName = pubDataNames[itemType];
		if (player.getBool(PLAYER_PROPERTY_ISIOS)&& (itemType == ITEM_TYPE.DIAMOND.ordinal() || itemType == ITEM_TYPE.GOLD.ordinal()||itemType == ITEM_TYPE.COUPONS.ordinal())) {
			pubName += "Ios";
		}
		IPubData pubData = kernel.getPubData(pubName);
		IPubRecord record = pubData.getRecord("Record");
		CustomMsg.StoreItems.Builder storeItems = CustomMsg.StoreItems.newBuilder();
		storeItems.setItemType(itemType);
		int rows = record.getRows();
		for (int i = 0; i < rows; i++) {
			String itemId = record.getString(i, ITEM_COL.COL_ITEM_ID.ordinal());
			int saleStatus = record.getInt(i, ITEM_COL.COL_SALE_STATUS.ordinal());
			if (itemType == ITEM_TYPE.GUN_SKIN.ordinal()) {
				boolean show = record.getBool(i, ITEM_BATTERY_COL.COL_SHOW.ordinal());
				if (!show) {
					continue;
				}
			}
			CustomMsg.StoreItem.Builder storeItem = CustomMsg.StoreItem.newBuilder();
			storeItem.setItemId(itemId);
			storeItem.setSaleStatus(saleStatus);
			int i1 = record.getInt(i, ITEM_EXCHANGE_COL.COL_PRICE.ordinal());
			// 如果是兑换物品有库存等字段
			if (itemType == ITEM_TYPE.EXCHANGE.ordinal()) {
				int salesCount = record.getInt(i, ITEM_EXCHANGE_COL.COL_SALES_COUNT.ordinal());
				int stock = record.getInt(i, ITEM_EXCHANGE_COL.COL_STOCK.ordinal());
				int price = record.getInt(i, ITEM_EXCHANGE_COL.COL_PRICE.ordinal());
				int sequence = record.getInt(i, ITEM_EXCHANGE_COL.COL_SEQUENCE.ordinal());
				boolean exchangeSwitch = record.getBool(i, ITEM_EXCHANGE_COL.COL_SWITCH.ordinal());
				if (!exchangeSwitch) {
					continue;
				}
				storeItem.setSalesCount(salesCount);
				storeItem.setStock(stock);
				storeItem.setPrice(price);
				storeItem.setSequence(sequence);
			} else if (itemType == ITEM_TYPE.EXCHANGE_CARD.ordinal()) {
				int salesCount = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_SALES_COUNT.ordinal());
				int stock = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_STOCK.ordinal());
				int price = record.getInt(i, ITEM_EXCHANGE_CARD_COL.COL_PRICE.ordinal());
				storeItem.setSalesCount(salesCount);
				storeItem.setStock(stock);
				storeItem.setPrice(price);
			} else if (itemType == ITEM_TYPE.EXCHANGE_VIP_INTEGRAL.ordinal()) {
				int salesCount = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_SALES_COUNT.ordinal());
				int stock = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_STOCK.ordinal());
				int price = record.getInt(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_PRICE.ordinal());
				String condition = record.getString(i, ITEM_EXCHANGE_VIP_INTEGRAL_COL.COL_CONDITION.ordinal());
				storeItem.setSalesCount(salesCount);
				storeItem.setStock(stock);
				storeItem.setPrice(price);
				storeItem.setMinVipLevel(Integer.parseInt(condition));
			}
//			logger.info("player:{}  获取商品列表{}=ItemId{} {} {}",player.getProperty("Uid"),pubName,storeItem.getItemId(),storeItem.getPrice(),storeItem.getSaleStatus());
			storeItems.addStoreItem(storeItem);
		}

		// logger.info("send S2C_STORE_ITEMS:" + storeItems.toString());
		kernel.sendMessage(player, S2CMsgDef.S2C_STORE_ITEMS.ordinal(), storeItems.build().toByteArray());

	}

	String OnCheckOrder(IKernel kernel, IGameObject player, int orderId) {
		IRecord buyGoodsRec = player.getRecord("BuyGoodsRec");
		int row = buyGoodsRec.findRow(0, 2, orderId);
		if (-1 != row) {
			return buyGoodsRec.getString(row, 3);
		}
		return null;
	}

	void removeRecordOrder(IKernel kernel, IGameObject player, int orderId) {
		if (orderId == 0){
			return;
		}
		IRecord buyGoodsRec = player.getRecord("BuyGoodsRec");
		int row = buyGoodsRec.findRow(0, 2, orderId);
		if (-1 != row) {
			buyGoodsRec.removeRow(row);
		}
	}

	public void InnerBuyStorePay(IKernel kernel, IGameObject player, CustomMsg.PreOrder preOrder) {
		InnerBuyStorePay(kernel, player, preOrder, false);
	}

	/**
	 * 支付请求
	 * @param kernel
	 * @param player
	 * @param preOrder
	 * @param discount
	 */
	public void InnerBuyStorePay(IKernel kernel, IGameObject player, CustomMsg.PreOrder preOrder, boolean discount) {
		com.alibaba.fastjson.JSONObject playerLog = kernel.getPlayerLog(player);
		playerLog.put("商品ID", preOrder.getGoodsId());
		try {
			if (preOrder.hasCashTicket()) {// 代金券是否足够
				int cashTicket = m_itemModule.GetItemCount(kernel, player, preOrder.getCashTicket());
				if (cashTicket <= 0) {
					CustomMsg.PreOrderResp.Builder preOrderResp = CustomMsg.PreOrderResp.newBuilder();
					preOrderResp.setResp("{\"code\":101,\"ID\":-1,\"extra\":{}}");
					kernel.sendMessage(player, S2CMsgDef.S2C_PREORDER_RESP.ordinal(), preOrderResp.build().toByteArray());
					return;
				}
			}
			// 限制1分钟内只能购买5次
			long curTime = kernel.getServerTime();
			int uid = player.getInt(PLAYER_PROPERTY_UID);
			PerOrderLimit plimit = m_listPerOrderLimit.get(uid);
			if (plimit == null){
				plimit = new PerOrderLimit();
				plimit.uid = uid;
				plimit.counts = 0;
				plimit.perTime = curTime;
				m_listPerOrderLimit.put(uid,plimit);
			}
			long passTime = curTime - plimit.perTime;
			if (passTime > 60000){//超过1分钟设置数据
				plimit.counts = 1;
				plimit.perTime = curTime;
			}else if (plimit.counts >= 5){
				CustomMsg.PreOrderResp.Builder preOrderResp = CustomMsg.PreOrderResp.newBuilder();
				preOrderResp.setResp("{\"code\":102,\"ID\":-1,\"extra\":{}}");
				kernel.sendMessage(player, S2CMsgDef.S2C_PREORDER_RESP.ordinal(), preOrderResp.build().toByteArray());
				return;
			}else{
				plimit.counts ++;
			}
			int index = preOrder.getIndex();
			int channelId = player.getInt(PLAYER_PROPERTY_CHANNEL);
			// 未成年防沉迷限制(不包含认证中的玩家)
			if (getChannelCertification(channelId) && player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME) == 0L){
				int age = player.getInt(PLAYER_PROPERTY_AGE);
				int monthCharge = player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT);
				// 游客不给充
				if (!player.getBool(PLAYER_PROPERTY_CERTIFICATION)){
					BuyStoreFailedRespClient(kernel, player, 106);
					return;
				}
				// 8岁以下不给充
				if (age < 8) {
					BuyStoreFailedRespClient(kernel, player, 103);
					return;
				}
				String goodsId = preOrder.getGoodsId();
				String cost = kernel.getCfgProperty(goodsId, "Cost"); // 价值即价格
				int price = Integer.parseInt(cost);
				// 8-16单次不超过50  月不超过200
				if (age < 16 && (price > 50 || monthCharge + price > 200)) {
					BuyStoreFailedRespClient(kernel, player, 104);
					return;
				}
				// 16-18单100 月400
				if (age >= 16 && age < 18 && (price > 100 || monthCharge + price > 400)) {
					BuyStoreFailedRespClient(kernel, player, 105);
					return;
				}
			}
			boolean testPay = SystemConfigData.getConfig("testPay",false) && player.getBool(PLAYER_PROPERTY_TESTPAY);
			String url  = SystemConfigData.getPayServerUrl("order/" + index);
			playerLog.put("testPay", testPay);
			playerLog.put("url", url);
			PreOrderCallBack callBack = new PreOrderCallBack(kernel, player);
			HttpClientUtil.doPost(kernel.getHttpClient(),url,getFormBody(preOrder,player, discount,testPay),callBack,callBack);
		} finally {
			logger.info(playerLog.toString());
		}
	}

	void BuyStoreFailedRespClient(IKernel kernel, IGameObject player, int code){
		CustomMsg.PreOrderResp.Builder preOrderResp = CustomMsg.PreOrderResp.newBuilder();
		preOrderResp.setResp("{\"code\":"+ code + "," + "\"ID\":-1,\"extra\":{}}");
		kernel.sendMessage(player, S2CMsgDef.S2C_PREORDER_RESP.ordinal(), preOrderResp.build().toByteArray());
	}
	public void OnBuyStoreGoodsByCoupons(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.String string = CustomMsg.String.parseFrom(data);
		JSONObject jsonObject = JSONObject.fromObject(string.getValue());
		String goodsId = jsonObject.getString("GoodId");
		int count = jsonObject.getInt("Count");
		logger.info("player:{} 玩家的点券数量{} 接收的数据{}",player.getProperty("Uid"),player.getProperty(PLAYER_PROPERTY_COUPONS),jsonObject);
		Integer price = canBuy4Coupons.get(goodsId);
		long result = player.getLong(PLAYER_PROPERTY_COUPONS) - price;
		boolean itemResult=false;


		logger.info("player:{} 购买商品 {} 扣除点券{} 剩余点券{} 发放结果{} ",player.getProperty("Uid"),goodsId+":"+count,price,result,itemResult,player.getProperty(PLAYER_PROPERTY_COUPONS));
		// 记录当前携带
		String orderLog = new StringBuilder().append("Coupons:").append(player.getProperty(PLAYER_PROPERTY_COUPONS)).append(",Gold:").append(player.getLong(PLAYER_PROPERTY_GOLD)).append(",Diamond:")
				.append(player.getLong(PLAYER_PROPERTY_DIAMOND)).append(",ColorTicket:").append(player.getLong(PLAYER_PROPERTY_COLORTICKET))
				.append(",item_powerstone:").append(m_itemModule.GetItemCount(kernel, player, "item_powerstone"))
				.append(",item_stone:").append(m_itemModule.GetItemCount(kernel, player, "item_stone"))
				.append(",item_mithril:").append(m_itemModule.GetItemCount(kernel, player, "item_mithril"))
				.append(",item_skill_bind_bomb:")
				.append(m_itemModule.GetItemCount(kernel, player, "item_skill_bind_bomb"))
				.append(",item_skill_normal_bomb:")
				.append(m_itemModule.GetItemCount(kernel, player, "item_skill_normal_bomb"))
				.append(",item_skill_missile:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_missile"))
				.append(",item_skill_nbomb:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_nbomb"))
				.append(",item_skill_hbomb:").append(m_itemModule.GetItemCount(kernel, player, "item_skill_hbomb"))
				.toString();
		kernel.addPlayLog(player, null, PlayLogType.PAY_ORDER, orderLog, "Buy " + goodsId);
		logger.info("player:{}  添加日志{} reason{}",player.getProperty("Uid"),orderLog, "Buy " + goodsId);
	}


	public void OnGetFreeCoupons(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		int vip = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if (player.getBool(PROPERTY_DAILY_COUPONS_STATE)){
			return;
		}
		String[] split = vipFreeAward.get(vip).split("\\*");
		boolean itemResult=false;
		if (split.length>1){
			itemResult= m_itemModule.AddItem(kernel, player, split[0], Integer.parseInt(split[1]), UtilFunc.System.GET_STORE_FREE_GOLD.ordinal(), "shop get free  gold");
			UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, split[0], Integer.parseInt(split[1]));
			player.setProperty(PROPERTY_DAILY_COUPONS_STATE,true);
			player.setProperty(PROPERTY_LAST_TIME_GET_COUPONS,kernel.getServerTime());
		}
		logger.info("player:{} VIP：{} 商城获取免费金币:{} 发放结果{}",player.getProperty("Uid"),vip,vipFreeAward.get(vip),itemResult);
	}
	public void OnBuyCoupons(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.PreOrder preOrder = CustomMsg.PreOrder.parseFrom(data);
		logger.info("player:{} 购买点券{} 发送订单执行InnerBuyStorePay",player.getProperty("Uid"));
		InnerBuyStorePay(kernel, player, preOrder, false);
	}
	public void OnBuyStorePay(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		//logger.info("OnBuyStorePay msgid:" + msgid);
		CustomMsg.PreOrder preOrder = CustomMsg.PreOrder.parseFrom(data);
		InnerBuyStorePay(kernel, player, preOrder, false);
	}

	private Map<String,Object> getFormBody(CustomMsg.PreOrder preOrder, IGameObject player, boolean discount,boolean testPay) {
		Map<String,Object> params = new HashMap<>();
		params.put("uid", player.getInt(PLAYER_PROPERTY_UID));
		params.put("goodsId", preOrder.getGoodsId());
		params.put("goodsType",0);
		//params.put("description",preOrder.getDescription());
		params.put("account",player.getString(PLAYER_PROPERTY_NAME));
		params.put("serverID",0);
		params.put("gameID",0);
		params.put("channelId",player.getInt(PLAYER_PROPERTY_CHANNEL));
		params.put("ip", player.getString("IP"));
		params.put("version",PLAYER_PROPERTY_VERSION);
		params.put("deviceId", player.getString("DeviceId"));
		params.put("macAddress", player.getString("MacAddress"));
		params.put("extra", preOrder.getExtra());
		params.put("discount",discount);
		params.put("payInfo", PLAYER_PROPERTY_PAYINFO);
		params.put("cashTicket", (preOrder.hasCashTicket() ? preOrder.getCashTicket() : ""));
		//内网模拟充值回调地址
		params.put("notifyHost",SystemConfigData.getConfig("payBackHost","127.0.0.1"));
		params.put("notifyPort",SystemConfigData.getConfig("payBackPort",8086));
		params.put("test",testPay);
		return params;
	}

	public boolean InnerBuyStoreItem(IKernel kernel, IGameObject player, String itemId, int count) {
		IPubData pubData = kernel.getPubData( pubDataNames[ITEM_TYPE.GUN_SKIN.ordinal()]);
		IPubRecord record = pubData.getRecord("Record");
		int rows = record.getRows();
		for (int i = 0; i < rows; i++) {
			String id = record.getString(i, ITEM_BATTERY_COL.COL_ITEM_ID.ordinal());
			if (id.equals(itemId)) {
				int price = record.getInt(i, ITEM_BATTERY_COL.COL_PRICE.ordinal()) * count;
				String properties = record.getString(i, ITEM_BATTERY_COL.COL_PROPERTIES.ordinal());
				long have = (long) player.getProperty(properties);
				if (have - price < 0) {
					logger.error("lack of " + properties + " can not buy item:" + itemId);
					return false;
				}
				player.setProperty(properties, have - price, UtilFunc.System.STORE.ordinal(),"InnerBuyStoreItem " + itemId + "*" + count);

				m_itemModule.AddItem(kernel, player, itemId, count, UtilFunc.System.STORE.ordinal(),"InnerBuyStoreItem"); // 物品加到背包里
				UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_PAY, itemId, count);
				return true;
			}
		}
		return false;
	}

	public void OnBuyStoreItem(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		//logger.info("OnBuyStoreItem msgid:" + msgid);
		CustomMsg.BuyStoreBattery buyStoreBattery = CustomMsg.BuyStoreBattery.parseFrom(data);
		if (buyStoreBattery.getItemType() != ITEM_TYPE.GUN_SKIN.ordinal()) {
			logger.error("OnBuyStoreItem Error type：" + buyStoreBattery.getItemType());
			return;
		}
		InnerBuyStoreItem(kernel, player, buyStoreBattery.getItemId(), 1);
	}

	void OnBuyStoreGoods(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
	}

	public void AddOrderResTip(IKernel kernel, IGameObject player, int orderId, ItemTipType type,Object... objects) {
		int uid  = player.getInt(PLAYER_PROPERTY_UID);
		logger.info("player:{} AddOrderResTip {} {} {} ",uid,orderId, type, objects);
		UtilFunc.sendItemTips(kernel, player, type, objects);
		OrderResTip tip = new OrderResTip();
		tip.type = type;
		tip.orderId = orderId;
		tip.objects = objects;
		m_mapOrderRes.put(uid,tip);
	}

	void OnReqOrderInfo(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)throws InvalidProtocolBufferException {
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		OrderResTip orderTip = m_mapOrderRes.remove(uid);
		if (orderTip != null){
			logger.info("player:{} OnReqOrderInfo {}  ", uid,orderTip.orderId);
		}else{
			logger.info("player:{} OnReqOrderInfo  no orderId",uid);
		}
		CustomMsg.String.Builder build = CustomMsg.String.newBuilder();
		build.setValue("");
		kernel.response(player,reqid,build.build().toByteArray());
	}

	void OnReqSetTempCellphone(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) throws InvalidProtocolBufferException {
		CustomMsg.String msg = CustomMsg.String.parseFrom(data);
		String cellphone = msg.getValue();
		if (StringUtils.isEmpty(cellphone)) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
			return;
		}
		if (StringUtils.isEmpty(player.getString(PROPERTY_FIRST_TEMP_CELLPHONE))) {
			player.setProperty(PROPERTY_FIRST_TEMP_CELLPHONE, cellphone);
		}
		player.setProperty(PROPERTY_LAST_TEMP_CELLPHONE, cellphone);
		UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_SUCCESS);
	}

	public void OnActivePush(IKernel kernel, int serid, int msgid, byte[] msg) {
		m_activePush = msg[0] == 1;
		logger.info("Set m_activePush {}", m_activePush);
	}

	private void loadDiamondAndGold(IKernel kernel, ICfgReader cfg) {
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			String itemId = cfg.getString(i, "Id");
			int price = cfg.getInt(i, "Price");
			payItems.put(itemId, price);
			canBuy4Coupons.put(itemId, price);
		}
	}
	private void loadCoupons(IKernel kernel, ICfgReader cfg) {
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			String itemId = cfg.getString(i, "Id");
			int price = cfg.getInt(i, "Price");
			payItems.put(itemId, price);
		}
	}
	private void loadVIPFree(IKernel kernel, ICfgReader cfg) {
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			int vip = cfg.getInt(i, "Id");
			String award = cfg.getString(i, "Award");
			vipFreeAward.put(vip, award);
		}
	}

	private void loadPkg(IKernel kernel, ICfgReader cfg) {
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			String itemId = cfg.getString(i, "Id");
			int price = cfg.getInt(i, "Price");
			boolean noDispatch = cfg.getBool(i, "NoDispatch");
			if (noDispatch) {
				noDispatchItems.add(itemId);
			}
			payItems.put(itemId, price);
			canBuy4Coupons.put(itemId, price);
		}
	}

	private void loadPreOrderUrl(IKernel kernel, ICfgReader cfg) {
		channelIntegral.clear();
		channelColorTicketDropRatio.clear();
		channelCertification.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			int channelId = cfg.getInt(i, "Id");
			String[] url = cfg.getStringArray(i, "Url", ";");
			channelIntegral.put(channelId, cfg.getInt(i, "Integral"));
			channelColorTicketDropRatio.put(channelId, cfg.getFloat(i,"LotteryProbability"));
			channelCertification.put(channelId, cfg.getBool(i,"Certification"));
		}
	}

	private void loadTest(IKernel kernel, ICfgReader cfg) {
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			String itemId = cfg.getString(i, "Id");
			int condition = cfg.getInt(i, "Condition");
			canBuy4TestItems.put(itemId, condition);
		}
	}

	private void loadExchange(IKernel kernel, ICfgReader cfg) {
		mapExchangeLimit.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			int[] limit = UtilFunc.parseIntArray(cfg.getString(i, "Limit"), ",");
			if (limit != null && limit.length >= 1) {
				mapExchangeLimit.put(cfg.getString(i, "Id"), limit);
			}
		}
	}

	private void loadExchangeCard(IKernel kernel, ICfgReader cfg) {
		mapExchangeCardLimit.clear();
		int count = cfg.getItemCount();
		for (int i = 0; i < count; i++) {
			int limit = cfg.getInt(i, "Limit");
			mapExchangeCardLimit.put(cfg.getString(i, "Id"), limit);
		}
	}

	public long addCurPubScore(IKernel kernel, int channelId,long addValue){
		if (addValue <= 0){
			return 0;
		}
		long newValue = addValue;
		if (channelCurPubSocre.containsKey(channelId)){
			newValue = newValue + channelCurPubSocre.get(channelId);
		}
		channelCurPubSocre.put(channelId,newValue);
		List<Object> params = new ArrayList<>();
		params.add(channelId);
		params.add(newValue);
		kernel.executeSomeToStore(PayChannelService.class,"updateCurScore",params,null);
		return newValue;
	}

	public long getCurPubScore(int channelId){
		if (channelCurPubSocre.containsKey(channelId)){
			return channelCurPubSocre.get(channelId);
		}
		return 0;
	}

	public void addMaxPubScore(IKernel kernel, int channelId,int addValue){
		if (addValue <= 0){
			return;
		}
		long newValue = addValue;
		if (channelMaxPubSocre.containsKey(channelId)){
			newValue = newValue + channelMaxPubSocre.get(channelId);
		}
		channelMaxPubSocre.put(channelId,newValue);
		List<Object> params = new ArrayList<>();
		params.add(channelId);
		params.add(newValue);
		kernel.executeSomeToStore(PayChannelService.class,"updateMaxScore",params,null);
	}

	public long getMaxPubScore(int channelId){
		if (channelMaxPubSocre.containsKey(channelId)){
			return channelMaxPubSocre.get(channelId);
		}
		return 0;
	}

	public int getChannelPubRatio(int channelId){
		if (channelPubRatio.containsKey(channelId)){
			return channelPubRatio.get(channelId);
		}
		return 0;
	}

	public int getChannelIntegral(int channelId){
		if (channelIntegral.containsKey(channelId)){
			return channelIntegral.get(channelId);
		}
		return 0; // 默认渠道的道具积分为5
	}

	public float getChannelColorTicketDropRatio(int channelId){
		if (channelColorTicketDropRatio.containsKey(channelId)){
			return channelColorTicketDropRatio.get(channelId);
		}
		return 5; // 默认渠道的彩券掉落几率
	}

	public boolean getChannelCertification(int channelId){
		if (channelCertification.containsKey(channelId)){
			return channelCertification.get(channelId);
		}
		return false; // 默认不是走官方实名认证
	}
}
