package game.modules.player;

import back.modules.data.player.DeductItemGameDTO;
import back.modules.data.player.OnlinePlayer;
import back.modules.dataenum.RoomType;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.JsonUtil;
import framework.PropertyKey;
import framework.SystemConfigData;
import framework.game.*;
import framework.net.HttpClientUtil;
import framework.pub.IPubData;
import game.constant.OfflineDataType;
import game.custommsg.*;
import game.modules.OfflineDataModule;
import game.modules.TimerManager;
import game.modules.fishgame.BulletValModule;
import game.modules.items.ItemModule;
import game.modules.store.StoreModule;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import okhttp3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * 描述： 玩家模块 创建人：胡中伟 创建时间：2018年3月12日 下午6:17:48
 *
 */
public class PlayerModule implements ILogicModule {

	public enum OptType {
		TYPE_UNKNOW,

		TYPE_CLICK_GOODS, // 点击物品
		TYPE_BUY_GOODS, // 购买物品
		TYPE_STAY_UI, // 停留在界面
		TYPE_CLICK_BV, // 点击解锁炮值

		TYPE_END
	}

	public class BombEdenState {
		int day;
		int open;
		long start;
		long stop;
	}

	enum FirstEntryRoomRecCol{
		COL_ID, // 房间ID
		COL_FLAG, // true: 第一次进入
		COL_END
	}

	private static Logger logger = LoggerFactory.getLogger(PlayerModule.class);
	private ItemModule m_itemModule;
	private BulletValModule m_BulletValModule;
	private OfflineDataModule m_OfflineDataModule;

	private static Map<String, Object> m_mapSensitiveWord = new HashMap<>();
	public static final String GOLD = PLAYER_PROPERTY_GOLD;
	public static final String TOTAL_PLAY = PLAYER_PROPERTY_TOTALPLAY;
	public static final String TOTAL_WIN = PLAYER_PROPERTY_TOTALWIN;
	public static final String FIRST_ENTRY_ROOM_REC = "FirstEntryRoomRec";

	public static final int CHANGE_NAME_DIAMOND = 50;
	private static final String FIRST_BIND_GIFT = "item_pkg_First_binding";
	private static final int REAL_NAME_AUTH_TIME = 5 * 60 * 1000; // 系统尝试实名认证时间间隔5分钟
	private static final int REAL_NAME_AUTH_OVER_TIME = 48 * 60 * 60 * 1000; // 尝试总时间48小时
	private static final String REAL_NAME_AUTH_TIMER = "HB_OnRealNameAuthTime";

	Map<Integer, String> m_mapBindUrls = new HashMap<>();
	String m_BindUrl = "";
	Map<Integer, BombEdenState> m_mapBombEdenOpen = new HashMap<>();
	TimerManager m_TimerManager = null;

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CLASS_READY, "Player", this, "OnPlayerClassReady");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
		kernel.regEvent(KernelEvent.KEVENT_ON_STANDUP, "Player", this, "OnPlayerStandUp");

		kernel.regRequestMessage(RequestMsgDef.REQ_HEADID.ordinal(), this, "OnReqHeadId");
		kernel.regRequestMessage(RequestMsgDef.REQ_HEADURL.ordinal(), this, "OnReqHeadUrl");
		kernel.regRequestMessage(RequestMsgDef.REQ_CHANGE_NAME.ordinal(), this, "OnReqChangeName");
		kernel.regRequestMessage(RequestMsgDef.REQ_PLAYER_DATA.ordinal(), this, "OnReqPlayerData");
		kernel.regRequestMessage(RequestMsgDef.REQ_BIND_CHANNEL.ordinal(), this, "OnRecvBind");
		kernel.regRequestMessage(RequestMsgDef.REQ_BOMB_EDEN_OPEN_CONFIG.ordinal(), this, "onReqBombEdenOpenConfig");
		kernel.regRequestMessage(RequestMsgDef.REQ_CHANGE_PASSWORD.ordinal(), this, "OnReqChangePassword");
		kernel.regRequestMessage(RequestMsgDef.REQ_CHANGE_PASSWORD_NEW.ordinal(), this, "OnReqChangePasswordNew");
		kernel.regRequestMessage(RequestMsgDef.REQ_BIND_PHONE.ordinal(), this, "OnReqBindPhone");
		kernel.regRequestMessage(RequestMsgDef.REQ_TOURIST_BIND_PHONE.ordinal(), this, "OnReqTouristBindPhone");
		kernel.regRequestMessage(RequestMsgDef.REQ_UN_BIND_PHONE.ordinal(), this, "OnReqUnBindPhone");
		kernel.regRequestMessage(RequestMsgDef.REQ_REAL_NAME.ordinal(), this, "OnRecvRealName");

		kernel.regServerRequest(ServerMsgDef.B2G_GET_ONLINE_PLAYER.ordinal(), this, "OnGetOnlinePlayer");
		kernel.regServerRequest(ServerMsgDef.B2G_DEDUCT_ITEMS.ordinal(), this, "OnDeductItems");
		kernel.regServerRequest(ServerMsgDef.B2G_ROOM_PLAYER_DATA.ordinal(), this, "OnReqRoomPlayer");
		kernel.regServerRequest(ServerMsgDef.B2G_GET_TOTAL_PW.ordinal(), this, "OnGetPlayerPW");
		kernel.regServerRequest(ServerMsgDef.B2G_SET_TOTAL_PW.ordinal(), this, "OnSetPlayerPW");//
		kernel.regServerRequest(ServerMsgDef.B2G_GET_PLAYER_TOTALWIN.ordinal(), this, "onGetPlayerTotalWin");
		kernel.regServerRequest(ServerMsgDef.B2G_GET_ONLINE_PLAYER_LIST.ordinal(), this, "OnGetOnlinePlayerList");
		kernel.regServerRequest(ServerMsgDef.B2G_GET_ONLINE_PLAYER_BombCount.ordinal(), this, "OnGetOnlinePlayerBombCount");
		kernel.regServerRequest(ServerMsgDef.B2G_REQ_DEDUCT_ITEM.ordinal(), this, "OnReqDeductItem");
        kernel.regServerRequest(ServerMsgDef.B2G_SET_INVITE_VIP.ordinal(), this, "OnSetPlayerInviteVip");

		kernel.regClientMessage(C2SMsgDef.C2S_SET_SIGN.ordinal(), this, "OnRecvSetSign");
		kernel.regClientMessage(C2SMsgDef.C2S_SET_LAST_OPT.ordinal(), this, "OnRecvSetOpt");
		//kernel.regClientMessage(C2SMsgDef.C2S_SET_HEAD.ordinal(), this, "OnRecvSetHeadId");
		kernel.regClientMessage(C2SMsgDef.C2S_SHOW_FIRST_TIP.ordinal(), this, "OnRecvFirstTip");
		kernel.regClientMessage(C2SMsgDef.C2S_USE_GUN_ADD_BET.ordinal(), this, "OnUseGunAddBet");
		kernel.regClientMessage(C2SMsgDef.C2S_BIND_PROXY_ID.ordinal(), this, "OnRecvBindProxy");
        kernel.regClientMessage(C2SMsgDef.C2S_PLAYER_PLACE.ordinal(), this, "OnReqPlayerPlace");

		kernel.listenPropertyChange(PLAYER_PROPERTY_NAME, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_SIGN, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_TITLEID, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_LEVEL, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_HEADID, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_BULLETLEVEL, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_BATTERYINUSE, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_HITFISHSCORE, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_HITEGGCOUNT, "Player", this, "OnPlayerProChanged");
		kernel.listenPropertyChange(PLAYER_PROPERTY_COLORTICKET, "Player", this, "OnPlayerProChanged");

		kernel.declareHeartBeat(REAL_NAME_AUTH_TIMER, this, "OnRealNameAuthTime");

		kernel.regCommand(CommandDef.CMD_REG_SUCCESS.ordinal(), "Player", this, "OnRegSuccess");

		m_itemModule = (ItemModule) kernel.getModule("ItemModule");
		m_BulletValModule = (BulletValModule) kernel.getModule("BulletValModule");
		m_OfflineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, "res/PreOrderUrl/PreOrderUrl.xml");
		RefreshCfg(kernel, "res/Config/BombEden.xml");
		RefreshCfg(kernel, "res/SensitiveWord/SensitiveWord.xml");
		m_BindUrl = SystemConfigData.getConfig("BindUrl","");
		m_TimerManager = (TimerManager) kernel.getModule("TimerManager");
		return true;
	}


	@Override
	public void onDestroy() {

	}

	void OnRegSuccess(IKernel kernel, IGameObject player,  Object... objects) {
		IRecord record = player.getRecord(FIRST_ENTRY_ROOM_REC);
		if (record == null) {
			return;
		}
		for (RoomType roomType : RoomType.values()) {
			record.addRow(roomType.ordinal(), true);
		}
	}

	void OnPlayerStandUp(IKernel kernel, IGameObject player, IGameObject desk){
		int type = desk.getInt(DESK_TYPE_KEY);
		IRecord record = player.getRecord(FIRST_ENTRY_ROOM_REC);
		if (record == null)
			return;
		int row = record.findRow(0, FirstEntryRoomRecCol.COL_ID.ordinal(), type);
		if (row != -1) {
			record.setValue(row, FirstEntryRoomRecCol.COL_FLAG.ordinal(), false);
		}
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/PreOrderUrl/PreOrderUrl.xml")) {
			m_mapBindUrls.clear();
			ICfgReader reader = kernel.loadXmlConfig(path);
			int count = reader.getItemCount();
			for (int i = 0; i < count; i++) {
				m_mapBindUrls.put(reader.getInt(i, "Id"), reader.getString(i, "BindUrl"));
			}
		} else if ("res/Config/BombEden.xml".equals(path)) {
			m_mapBombEdenOpen.clear();
			ICfgReader cfg = kernel.loadXmlConfig("res/Config/BombEden.xml");
			if (cfg == null) {
				return;
			}

			int count = cfg.getItemCount();
			for (int i = 0; i < count; ++i) {
				int day = cfg.getInt(i, "Id");
				int open = cfg.getInt(i, "Open");
				long start = 0;
				long stop = 0;
				try {
					start = UtilFunc.timeParse(cfg.getString(i, "Start"));
					stop = UtilFunc.timeParse(cfg.getString(i, "Stop"));
				} catch (ParseException e) {
					logger.error("BombEden time load error");
					start = kernel.getServerTime();
					stop = kernel.getServerTime();
				}
				BombEdenState state = new BombEdenState();
				state.day = day;
				state.open = open;
				state.start = start;
				state.stop = stop;
				m_mapBombEdenOpen.put(day, state);
			}
		} else if ("res/SensitiveWord/SensitiveWord.xml".equals(path)) {
			loadSensitiveWord(kernel, path);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void loadSensitiveWord(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if (cfg == null) {
			return;
		}
		m_mapSensitiveWord.clear();
		int count = cfg.getItemCount();
		String key = null;
		Map nowMap = null;
		Map<String, String> newWorMap = null;
		for (int j = 0; j < count; ++j) {
			key = cfg.getString(j, "Key").trim(); // 关键字
			nowMap = m_mapSensitiveWord;
			for (int i = 0; i < key.length(); i++) {
				char keyChar = key.charAt(i); // 转换成char型
				Object wordMap = nowMap.get(keyChar); // 获取

				if (wordMap != null) { // 如果存在该key，直接赋值
					nowMap = (Map) wordMap;
				} else { // 不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
					newWorMap = new HashMap<>();
					newWorMap.put("isEnd", "0"); // 不是最后一个
					nowMap.put(keyChar, newWorMap);
					nowMap = newWorMap;
				}
				if (i == key.length() - 1) {
					nowMap.put("isEnd", "1"); // 最后一个
				}
			}
		}
	}

    // 允许：中文（\u4e00-\u9fa5）、英文、数字
    public static boolean isValid(String text) {
        if (text == null) return false;
        return text.matches("^[A-Za-z0-9\\u4e00-\\u9fa5]+$");
    }

    @SuppressWarnings("rawtypes")
    public static boolean checkSensitiveWord(String txt) {
        boolean flag = false;
        if (!isValid(txt)) {
            return true;
        }
		Map nowMap = m_mapSensitiveWord;
		boolean child = false;
		for (int i = 0; i < txt.length(); i++) {
			nowMap = (Map) nowMap.get(txt.charAt(i)); // 获取指定key
			if (nowMap != null) { // 存在，则判断是否为最后一个
				child = true;
				if ("1".equals(nowMap.get("isEnd"))) { // 包含敏感词
					flag = true; // 结束标志位为true
					break;
				}
			} else {
				if (child) {
					child = false;
					i--;
				}
				nowMap = m_mapSensitiveWord;
			}
		}
		return flag;
	}

	@SuppressWarnings("rawtypes")
	public String replaceSensitiveWord(String txt) {
		Map nowMap = m_mapSensitiveWord;
		StringBuilder stringBuilder = new StringBuilder();
		boolean child = false;
		for (int i = 0; i < txt.length(); i++) {
			char word = txt.charAt(i);
			nowMap = (Map) nowMap.get(word); // 获取指定key
			if (nowMap != null) { // 存在，则判断是否为最后一个
				child = true;
				stringBuilder.append(word);
				if ("1".equals(nowMap.get("isEnd"))) { // 包含敏感词
					StringBuilder star = new StringBuilder();
					for (int k = 0; k < stringBuilder.length(); k++) {
						star.append("*");
					}
					txt = txt.replaceAll(stringBuilder.toString(), star.toString());
					stringBuilder.delete(0, stringBuilder.length());
				}
			} else {
				if (child) {
					child = false;
					stringBuilder.delete(0, stringBuilder.length());
					i--;
				}
				nowMap = m_mapSensitiveWord;
			}
		}
		return txt;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_ACTIVITY_SPEED, ValueType.FLOAT, false, true, true);
		kernel.declareProperty(script, GOLD, ValueType.LONG, true, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_COUPONS, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_DIAMOND, ValueType.LONG, true, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_OFFPROTECT, ValueType.INT, false, false, false);
		kernel.declareProperty(script, TOTAL_PLAY, ValueType.LONG, false, false, true);
		kernel.declareProperty(script, TOTAL_WIN, ValueType.LONG, false, false, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_LEVEL, ValueType.INT, true, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_EXP, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_COLORTICKET, ValueType.LONG, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALCOLORTICKET, ValueType.LONG, false, false, true); //累计获得话费券
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALUSECOLORTICKET, ValueType.LONG, false, false, true); //累计消耗话费券
		kernel.declareProperty(script, PLAYER_PROPERTY_SHUTUP, ValueType.LONG, false, false, true);//禁言失效时间
		kernel.declareProperty(script, PLAYER_PROPERTY_LASTLOGINTIME, ValueType.LONG, false, false, true);//上次登录时间
		kernel.declareProperty(script, PLAYER_PROPERTY_DAYRELIEFCOUNT, ValueType.INT, false, true, true);//每日救济金已领取次数
		kernel.declareProperty(script, PLAYER_PROPERTY_HAVEBOMBSCORE, ValueType.INT, false, true, true); //拥有的炸弹积分
		kernel.declareProperty(script, PLAYER_PROPERTY_HITFISHSCORE, ValueType.LONG, false, true, true); //捕鱼得分
		kernel.declareProperty(script, PLAYER_PROPERTY_SIGN, ValueType.STRING, true, true, true); //签名信息
		kernel.declareProperty(script, PLAYER_PROPERTY_FREECHANGENAME, ValueType.INT, false, true, true); //免费签名剩余次数
		kernel.declareProperty(script, PLAYER_PROPERTY_ISNEW, ValueType.BOOL, false, true, true); //免费签名剩余次数
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALDEBRISGOLDENSTORM, ValueType.INT, false, false, true); //累计炮台碎片数
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALDEBRISCHUXI, ValueType.INT, false, false, true); //累计除夕炮台碎片数
		kernel.declareProperty(script, PLAYER_PROPERTY_REALNAME, ValueType.BOOL, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_IDENTITYCARD, ValueType.STRING, false, false, true);//身份证
		kernel.declareProperty(script, PLAYER_PROPERTY_BINDCHANNEL, ValueType.STRING, false, true, true);
		kernel.declareProperty(script, PLAYER_PROPERTY_INFOBG, ValueType.INT, true, true, true); //信息背景框
		kernel.declareProperty(script, PLAYER_PROPERTY_FIRSTINROOM, ValueType.INT, false, true, true); //新玩家首次进入房间
		kernel.declareProperty(script, PLAYER_PROPERTY_ISIOS, ValueType.BOOL, false, false, false); //新玩家首次进入房间
		kernel.declareProperty(script, PLAYER_PROPERTY_BOSSBETADD, ValueType.BOOL, false, true, true); //BOSS倍率提升
		//kernel.DeclareProperty(script, PLAYER_PROPERTY_CITY, ValueType.VTYPE_STRING, false, true, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_INITCOLORTICKET, ValueType.BOOL, false, false, false); //记录ColorTicket初始是否赋值
		kernel.declareProperty(script, PLAYER_PROPERTY_BINDPROXY, ValueType.BOOL, false, true, true);// 玩家是否绑定过代理
		kernel.declareProperty(script, PLAYER_PROPERTY_SKILLMULTIPLE, ValueType.INT, true, true, false);//玩家技能属性
		kernel.declareProperty(script, PLAYER_PROPERTY_BINDPHONE_BEFORE, ValueType.INT, false, true, true);//绑定手机号标志 只要绑定了一次就有
		kernel.declareProperty(script, PLAYER_PROPERTY_MAIL_CAN_SEND_NUM, ValueType.INT, false, true, true);//可以邮寄道具的数量
		kernel.declareProperty(script, PLAYER_PROPERTY_BOMBTOTALPLAY, ValueType.LONG, false, true, true);// 魔晶场玩家消耗(仅捕鱼)
		kernel.declareProperty(script, PLAYER_PROPERTY_BOMBTOTALWIN, ValueType.LONG, false, false, true);// 魔晶场玩家总赢
		kernel.declareProperty(script, PLAYER_PROPERTY_PROPERTY_PROXY_ID, ValueType.INT, false, false, true);//玩家的代理编号
		kernel.declareProperty(script, PLAYER_PROPERTY_REGISTER_TYPE, ValueType.INT, false, true, true);//玩家注册方式
		kernel.declareProperty(script, PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME, ValueType.LONG, false, true, true);//认证中状态下 系统开始尝试认证的时间戳
		kernel.declareProperty(script, PLAYER_PROPERTY_LAST_SAVE, ValueType.LONG, false, false, false);//ty上次存档时间
		kernel.declareProperty(script, PLAYER_PROPERTY_HAVENBOMB_SCORE, ValueType.INT, false, true, true); //拥有的至尊三叉戟积分
		kernel.declareProperty(script, PLAYER_PROPERTY_HAVEHBOMB_SCORE, ValueType.INT, false, true, true); //使用的传说三叉戟积分
		kernel.declareProperty(script, PLAYER_PROPERTY_GOLD_BURST, ValueType.LONG, false, true, true); //
		kernel.declareProperty(script, PLAYER_PROPERTY_BOMBCOIN_BURST, ValueType.LONG, false, true, true); //
		kernel.declareProperty(script, PLAYER_HIT_FISH_LAST_TIME, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_SKILL_SPEED_END_TIME, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_ON_ENTER_ROOM_END_TIME, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_CURRENT_PLACE, ValueType.INT, false, true, false); //
		kernel.declareProperty(script, PLAYER_SHZ_SINGLE_SCORE, ValueType.LONG, false, true, true); //
		kernel.declareProperty(script, PLAYER_SHZ_BET_NUM, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_SHZ_LAST_TIME, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_SHZ_BONUS_NUM, ValueType.INT, false, true, false); //
		kernel.declareProperty(script, PLAYER_SHZ_MARRY_NUM, ValueType.INT, false, true, false); //
		kernel.declareProperty(script, PLAYER_SHZ_OBJECT, ValueType.OBJECT, false, true, false); //
		kernel.declareProperty(script, PLAYER_INVITER_VIP_INFO_CACHE, ValueType.OBJECT, false, true, false); //
		kernel.declareProperty(script, PLAYER_INVITER_VIP_INFO_CACHE_TIME, ValueType.LONG, false, true, false); //
		kernel.declareProperty(script, PLAYER_INVITER_VIP_STATUS, ValueType.INT, false, true, false); //
		kernel.declareProperty(script, PLAYER_INVITER_BIND_ID, ValueType.INT, false, true, false); //
		kernel.declareProperty(script, PLAYER_GUN_EQUIP, ValueType.INT, true, true, true); //
		kernel.declareProperty(script, PLAYER_GUN_OWNED, ValueType.STRING, false, false, true); //

		IRecord record = kernel.declareRecord(script, FIRST_ENTRY_ROOM_REC, FirstEntryRoomRecCol.COL_END.ordinal(), RoomType.ROOM_END.ordinal(), false, true, true);
		record.setColType(FirstEntryRoomRecCol.COL_ID.ordinal(), ValueType.INT);
		record.setColType(FirstEntryRoomRecCol.COL_FLAG.ordinal(), ValueType.BOOL);
	}

	public void OnPlayerClassReady(IKernel kernel, String script) {
		kernel.setVisible(script, PLAYER_PROPERTY_NAME, true, true, false);
		kernel.setVisible(script, PLAYER_PROPERTY_UID, true, true, false);
		kernel.setVisible(script, PLAYER_PROPERTY_HEADID, true, true, true);
		kernel.setVisible(script, PLAYER_PROPERTY_SEX, true, true, false);
		kernel.setVisible(script, PLAYER_PROPERTY_DESKID, false, true, false);
		kernel.setVisible(script, PLAYER_PROPERTY_SEATID, true, true, false);
		kernel.setVisible(script, PLAYER_PROPERTY_CHANNEL, true, true, false);
		//玩家登陆IP对客户端可视，用于客户端自主获取城市信息 add by 胡中伟, 2019年3月20日 下午2:40:33
		kernel.setVisible(script, PLAYER_PROPERTY_IPADDR, false, true, false);
		//注册日期对客户端可视，用于风控系统计算注册天数 add by 胡中伟, 2019年3月25日 下午2:41:32
		kernel.setVisible(script, PLAYER_PROPERTY_REGTIME, false, true, false);
	}

	void OnUseGunAddBet(IKernel kernel, IGameObject player, int msgId, byte[] data) throws InvalidProtocolBufferException {
		CustomMsg.Int32 msg = CustomMsg.Int32.parseFrom(data);
		player.setProperty(PLAYER_PROPERTY_BOSSBETADD, msg.getValue() == 1);
	}

	public void OnReqHeadId(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.GetHeadId msgData = CustomMsg.GetHeadId.parseFrom(msg);
		int uid = msgData.getUid();
		int headid = kernel.getUserHeadid(uid);

		CustomMsg.HeadIdRes.Builder res = CustomMsg.HeadIdRes.newBuilder();
		res.setHeadid(headid);

		kernel.response(player, reqid, res.build().toByteArray());
	}

	public void OnReqHeadUrl(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.GetHeadUrl msgData = CustomMsg.GetHeadUrl.parseFrom(msg);
		int headId = msgData.getHeadid();
		String headUrl = kernel.getHeadUrl(headId);
		CustomMsg.HeadUrlRes.Builder res = CustomMsg.HeadUrlRes.newBuilder();
		res.setHead(headUrl == null ? "" : headUrl);
		kernel.response(player, reqid, res.build().toByteArray());
	}

	void OnReqChangeName(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		CustomMsg.ReqChangeName msgData = CustomMsg.ReqChangeName.parseFrom(msg);
		String newName = msgData.getName();
		if (checkSensitiveWord(newName)) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);// 包含敏感词
			return;
		}
//		boolean useFree = false;
//		int freeCount = player.getInt(PLAYER_PROPERTY_FREECHANGENAME);
//		long diamond = player.getLong(PLAYER_PROPERTY_DIAMOND);
//		if (freeCount > 0) {
//			useFree = true;
//			--freeCount;
//			player.setProperty(PLAYER_PROPERTY_FREECHANGENAME, freeCount);
//		}
//		else if (diamond >= CHANGE_NAME_DIAMOND) {
//			diamond -= CHANGE_NAME_DIAMOND;
//			player.setProperty(PLAYER_PROPERTY_DIAMOND, diamond, UtilFunc.System.PLAYER.ordinal(),"Change name " + player.getString(PLAYER_PROPERTY_NAME) + "->" + newName);
////			m_WarningModule.UseDiamond(player.GetInt(PLAYER_PROPERTY_UID), CHANGE_NAME_DIAMOND);
//		} else {
//			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_NOT_ENOUGH);
//			return;
//		}
//		boolean useFreeFinal = useFree;
		kernel.changeName(player, newName, (res) -> {
//			if (!res) {
//				if (useFreeFinal) {
//					player.setProperty(PLAYER_PROPERTY_FREECHANGENAME, player.getInt(PLAYER_PROPERTY_FREECHANGENAME) + 1);
//				} else {
//					player.setProperty(PLAYER_PROPERTY_DIAMOND, player.getLong(PLAYER_PROPERTY_DIAMOND) + CHANGE_NAME_DIAMOND,UtilFunc.System.PLAYER.ordinal(),"Change name failed " + player.getString(PLAYER_PROPERTY_NAME) + "->" + newName);
////					m_WarningModule.AddDiamond(player.GetInt(PLAYER_PROPERTY_UID), CHANGE_NAME_DIAMOND);
//				}
//			}
			UtilFunc.responseSerCode(kernel, player, reqid,res ? ServerCodeDef.CODE_SUCCESS : ServerCodeDef.CODE_FAILED);
		});
	}

	void OnReqPlayerData(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) throws InvalidProtocolBufferException {
		CustomMsg.Uid msgData = CustomMsg.Uid.parseFrom(msg);
		int uid = msgData.getUid();
		String pubdataName = "Player_" + uid;

		CustomMsg.PlayerData.Builder build = CustomMsg.PlayerData.newBuilder();
		do {
			IPubData pubdata = kernel.getPubData(pubdataName);
			if (pubdata == null) {
				build.setCode(ServerCodeDef.CODE_UID_NOT_EXIST.ordinal());
				break;
			}

			build.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
			build.setName(pubdata.getString(PLAYER_PROPERTY_NAME));
			build.setSign(pubdata.getString(PLAYER_PROPERTY_SIGN));
			build.setTitle(pubdata.getString(PLAYER_PROPERTY_TITLEID));
			build.setLevel(pubdata.getInt(PLAYER_PROPERTY_LEVEL));
			build.setLevel(pubdata.getInt(PLAYER_PROPERTY_LEVEL));
			build.setHeadid(pubdata.getInt(PLAYER_PROPERTY_HEADID));
			build.setBet(pubdata.getInt("Bet"));
			build.setScore(pubdata.getInt("Score"));
			build.setSkin(pubdata.getInt("Skin"));
			build.setMaxScore(pubdata.getInt("MaxScore"));
		} while (false);
		kernel.response(player, reqid, build.build().toByteArray());
	}

	void onReqBombEdenOpenConfig(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(kernel.getServerTime());
		int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if (day == 0)
			day = 7;
		BombEdenState state = m_mapBombEdenOpen.get(day);
		CustomMsg.BombEdenConfig.Builder builder = CustomMsg.BombEdenConfig.newBuilder();
		builder.setOpen(state.open);
		long zeroTime = UtilFunc.getZeroTime(calendar.getTimeInMillis());
		builder.setStartTimestamp(zeroTime + state.start);
		builder.setStopTimestamp(zeroTime + state.stop);
		kernel.response(player, reqid, builder.build().toByteArray());
	}

	void OnRecvBind(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.String val = CustomMsg.String.parseFrom(data);
		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		if (!m_mapBindUrls.containsKey(channel)) {
			logger.info("!m_mapBindUrls.containsKey(channel) " + channel);
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
			return;
		}

		String url = m_mapBindUrls.get(channel);

		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("http://").append(m_BindUrl).append(url);
		logger.info("OnRecvBind Url: " + urlBuilder.toString());

		FormBody.Builder formBody = new FormBody.Builder();
		formBody.add("uid", String.valueOf(player.getInt(PLAYER_PROPERTY_UID)));
		formBody.add("info", val.getValue());

		Request request = new Request.Builder().url(urlBuilder.toString()).post(formBody.build()).build();

		player.addTempData("BindReqID", ValueType.INT, reqid);

		httpClient.newCall(request).enqueue(new BindCallBack(kernel, player.getInt(PLAYER_PROPERTY_UID), player.getObjectID()));
	}

	void OnReqChangePassword(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.ChangePassword val = CustomMsg.ChangePassword.parseFrom(data);
		String url = SystemConfigData.getLoginServerUrl("changePassword");
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid",uid);
		map.put("oldPassword", val.getOldPassword());
		map.put("newPassword", val.getNewPassword());
		ChangePWCallBack callBack = new ChangePWCallBack(kernel,uid,player.getObjectID());
		logger.info("玩家更改验证码 {}",map);
		player.addTempData("ChangePasswordReqID", ValueType.INT, reqid);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}
	void OnReqChangePasswordNew(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.String val = CustomMsg.String.parseFrom(data);
		logger.info(val.getValue());
		JSONObject msg = JSONObject.parseObject(val.getValue());
		String url = SystemConfigData.getLoginServerUrl("changePassword");
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid",uid);
		map.put("oldPassword", msg.get("oldPassword"));
		map.put("newPassword", msg.get("newPassword"));
		map.put("verify", msg.get("verify"));
		logger.info("玩家请求更改密码 {}",map);
		ChangePWCallBack callBack = new ChangePWCallBack(kernel,uid,player.getObjectID());
		player.addTempData("ChangePasswordReqID", ValueType.INT, reqid);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}

	void OnReqBindPhone(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.BindPhone val = CustomMsg.BindPhone.parseFrom(data);
		if (StringUtils.isNotBlank(player.getString(PLAYER_PROPERTY_PHONE))) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
			return;
		}
		String url = SystemConfigData.getLoginServerUrl("bindPhone");
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid", uid);
		map.put("phone", val.getPhone());
		map.put("password", val.getPassword());
		map.put("messageCode", val.getMessageCode());
		BindPhoneCallBack callBack = new BindPhoneCallBack(kernel,uid, player.getObjectID(),val.getPhone());
		player.addTempData("BindPhoneReqID", ValueType.INT, reqid);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}

	// 游客请求绑定手机号
	void OnReqTouristBindPhone(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.BindPhone val = CustomMsg.BindPhone.parseFrom(data);
		if (StringUtils.isNotBlank(player.getString(PLAYER_PROPERTY_PHONE))) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
			return;
		}
		String url = SystemConfigData.getLoginServerUrl("touristBindPhone");
		String reqInfo = val.getPhone();
		JsonObject json = JsonUtil.decodeToObj(reqInfo, JsonObject.class);
		String phone = json.get("phone").getAsString();
		player.addTempData("BindPhoneReqID", ValueType.INT, reqid);
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid", uid);
		map.put("phone", phone);
		map.put("password", json.get("password").getAsString());
		map.put("accountNumber", json.get("accountNumber").getAsString());
		map.put("messageCode", json.get("messageCode").getAsString());
		BindPhoneCallBack callBack = new BindPhoneCallBack(kernel,uid, player.getObjectID(),phone);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}

	void OnReqUnBindPhone(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
//		CustomMsg.UnBindPhone val = CustomMsg.UnBindPhone.parseFrom(data);
		JSONObject val = JSONObject.parseObject(CustomMsg.String.parseFrom(data).getValue());
		if (StringUtils.isBlank(player.getString(PLAYER_PROPERTY_PHONE))) {
			UtilFunc.responseSerCode(kernel, player, reqid, ServerCodeDef.CODE_WRONG_STATE);
			return;
		}
		String url = SystemConfigData.getLoginServerUrl("unBindPhone");
		player.addTempData("UnBindPhoneReqID", ValueType.INT, reqid);
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid", uid);
		map.put("phone", val.get("newphone"));
		map.put("oldphone", val.get("oldphone"));
		map.put("username", player.getProperty("Name"));
		map.put("messageCode", val.get("messageCode"));
		map.put("oldmessageCode", val.get("oldmessageCode"));
		logger.info(val.toJSONString());
		UnBindPhoneCallBack callBack = new UnBindPhoneCallBack(kernel,uid, player.getObjectID());
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}

	public void OnGetOnlinePlayer(IKernel kernel, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.IntArray array = ServerMsg.IntArray.parseFrom(data);
		ServerMsg.PlayerInfo.Builder build = ServerMsg.PlayerInfo.newBuilder();
		for (int i = 0 ; i < array.getIdCount(); i++) {
			int uid = array.getId(i);
			IGameObject player = kernel.getPlayer(uid);
			if (player == null){
				continue;
			}
			build.addPlayerId(uid);
			String name = player.getString(PLAYER_PROPERTY_NAME);
			int sex = player.getInt(PLAYER_PROPERTY_SEX);
			int level = player.getInt(PLAYER_PROPERTY_LEVEL);
			int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			long gold = player.getLong(PLAYER_PROPERTY_GOLD);
			long mojin = player.getLong(PLAYER_PROPERTY_BOMB_COIN);
			long diamond = player.getLong(PLAYER_PROPERTY_DIAMOND);
			long lottery = player.getLong(PLAYER_PROPERTY_COLORTICKET);
			// regDate will be acquire by backweb from db
			long loginDate = player.getLong(PLAYER_PROPERTY_LASTLOGINTIME);
			int placeId = player.getInt(PLAYER_PROPERTY_CHANNEL);
			IGameObject itemBag = player.getContainer("ItemBag");
			String prop = "";
			if (itemBag != null) {
				int cap = itemBag.getCapacity();
				for (int j = 0 ; j < cap; j++) {
					IGameObject item = itemBag.getChild(j);
					if (item == null) {
						continue;
					}
					String itemId = item.getString("Id");
					int count = item.getInt("Count");
					prop += (itemId + "*" + count + " ");
				}
			}
			long shutUp = player.getLong(PLAYER_PROPERTY_SHUTUP);
			int totalRechargeAmount = player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT);
			long deskId = player.getLong(PLAYER_PROPERTY_DESKID);
			IGameObject deskGO = kernel.getGameObject(deskId);
			int online = -1;
			if (deskGO != null) {
				online = deskGO.getInt("Type");
			}
			int bulletLevel = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
			int nBulletLevel = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
			String bank = "ICBC";
			build.addName(name);
			build.addSex(sex);
			build.addLevel(level);
			build.addVipLevel(vipLevel);
			build.addGold(gold);
			build.addDiamond(diamond);
			build.addLottery(lottery);
			build.addLoginDate(loginDate);
			build.addPlaceId(placeId);
			build.addProp(prop);
			build.addShutUp(shutUp);
			build.addTotalCharge(totalRechargeAmount);
			build.addStatus(0);
			build.addOnline(online);
			build.addMaxGun(bulletLevel);
			build.addBank(bank);
			build.addTempCellphone(player.getString(StoreModule.PROPERTY_LAST_TEMP_CELLPHONE));
			build.addNMaxGun(nBulletLevel);
			build.addMojin(mojin);
			build.addGoldTotalPlay(player.getLong(PLAYER_PROPERTY_TOTALPLAY));
			build.addGoldTotalWin(player.getLong(PLAYER_PROPERTY_TOTALWIN));
			build.addBombTotalPlay(player.getLong(PLAYER_PROPERTY_BOMBTOTALPLAY));
			build.addBombTotalWin(player.getLong(PLAYER_PROPERTY_BOMBTOTALWIN));
		}
		kernel.responseServer(msgid, build.build().toByteArray());
	}

	void OnGetPlayerPW(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.IntSingle request = ServerMsg.IntSingle.parseFrom(data);
		int uid = request.getIntMember();

		ServerMsg.TotalPW.Builder build = ServerMsg.TotalPW.newBuilder();
		IGameObject player = kernel.getPlayer(uid);

		if (player != null) {
			IRecord rec = player.getRecord("TotalPlayWin");
			int row = rec.getRows();
			for (int i = 0; i < row; ++i) {
				build.addPlay(rec.getLong(i, 0));
				build.addWin(rec.getLong(i, 1));
			}
		}
		kernel.responseServer(reqid, build.build().toByteArray());
	}

	void OnSetPlayerPW(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.TotalPW request = ServerMsg.TotalPW.parseFrom(data);
		int uid = request.getUid();
		IGameObject player = kernel.getPlayer(uid);

		if (player != null) {
			IRecord rec = player.getRecord("TotalPlayWin");
			int count = request.getPlayCount();
			for (int i = 0; i < count; ++i) {
				int id = request.getId(i);
				rec.setValue(id, 0, request.getPlay(i));
				rec.setValue(id, 1, request.getWin(i));
			}
		}

		kernel.responseServer(reqid, null);
	}

    void OnSetPlayerInviteVip(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
        logger.info("OnSetPlayerInviteVip");
        ServerMsg.InviteVip request = ServerMsg.InviteVip.parseFrom(data);
        int uid = request.getUid();
        IGameObject player = kernel.getPlayer(uid);
        if (player != null) {
            player.setProperty(PropertyKey.PLAYER_INVITER_VIP_STATUS, request.getVip());
            CustomMsg.Int32.Builder build = CustomMsg.Int32.newBuilder();
            build.setValue(request.getVip());
            kernel.sendMessage(player, S2CMsgDef.S2C_PLAYER_GET_INVITE_VIP.ordinal(), build.build().toByteArray());
        }
        kernel.responseServer(reqid, null);
    }

	public void OnDeductItems(IKernel kernel, int msgid, byte[] data) throws InvalidProtocolBufferException {
		logger.info("OnDeductItems");
		ServerMsg.IntAndString request = ServerMsg.IntAndString.parseFrom(data);
		int uid = request.getIntMember();
		String props = request.getStrMember();
		IGameObject player = kernel.getPlayer(uid);
		ServerMsg.IntAndString.Builder build = ServerMsg.IntAndString.newBuilder();
		String[] propArray = props.split(" ");
		String propsRes = "";
		int resInt = 0;
		for (String prop : propArray) {// 检查够不够
			String[] detail = prop.split(":");
			String itemName = detail[0];
			int del = Integer.valueOf(detail[1]);
			if (itemName.startsWith("item_")) {
				int count = m_itemModule.GetItemCount(kernel, player, itemName);
				if (count < del) {
					resInt = -1;
					propsRes += itemName + ":" + count + " ";
				}
			} else if (itemName.equals(PLAYER_PROPERTY_GOLD) || itemName.equals(PLAYER_PROPERTY_DIAMOND) || itemName.equals(PLAYER_PROPERTY_COLORTICKET)
					|| itemName.equals(PLAYER_PROPERTY_ITEMSCORE) || itemName.equals(PLAYER_PROPERTY_RECHARGESCORE)) {
				long count = player.getLong(itemName);
				if (count < del) {
					resInt = -1;
					propsRes += itemName + ":" + count + " ";
				}
			}
		}
		if (resInt >= 0) {// 够扣
			for (String prop : propArray) {
				String[] detail = prop.split(":");
				String itemName = detail[0];
				int del = Integer.valueOf(detail[1]);
				if (itemName.startsWith("item_")) {
					m_itemModule.SubItem(kernel, player, itemName, del, UtilFunc.System.BACK_DEDUCT_ITEM.ordinal(), "back deduct item");
//					ItemLogModule.AddItemLog(kernel,player,itemName,del, ItemLogEnum.BACK_DEDUCT_ITEM.ordinal());
					// 魔晶预警-后台扣除道具
				} else if (itemName.equals(PLAYER_PROPERTY_GOLD) || itemName.equals(PLAYER_PROPERTY_DIAMOND) || itemName.equals(PLAYER_PROPERTY_COLORTICKET)
						|| itemName.equals(PLAYER_PROPERTY_ITEMSCORE) || itemName.equals(PLAYER_PROPERTY_RECHARGESCORE)) {
					long count = player.getLong(itemName);
					if (count > del) {
						count -= del;
						player.setProperty(itemName, count, UtilFunc.System.PLAYER.ordinal(), "back deduct property");
					}
				}
			}
		}
		propsRes.trim();
		build.setIntMember(resInt);
		build.setStrMember(propsRes);
		kernel.responseServer(msgid, build.build().toByteArray());
	}

	void OnPlayerProChanged(IKernel kernel, IGameObject player, String proName, Object oldVal) {
		// 玩家话费券计入玩家总得 add by 赵俊 @20190420
		if (PLAYER_PROPERTY_COLORTICKET.equals(proName)) {
			long colorTicket = player.getLong(PLAYER_PROPERTY_COLORTICKET);
			if (colorTicket <= (long) oldVal) {// 少了
				// 消耗了ColorTicket计入总消耗 add by 赵俊 @20190430
				player.setProperty(PLAYER_PROPERTY_TOTALUSECOLORTICKET,
						player.getLong(PLAYER_PROPERTY_TOTALUSECOLORTICKET) + ((long) oldVal - colorTicket));
				return;
			}
			if (player.getBool(PLAYER_PROPERTY_INITCOLORTICKET)) {
				player.setProperty(PLAYER_PROPERTY_TOTALWIN, player.getLong(PLAYER_PROPERTY_TOTALWIN) + (colorTicket - (long) oldVal) * 100);
			} else {
				player.setProperty(PLAYER_PROPERTY_INITCOLORTICKET, true);
				player.setProperty(PLAYER_PROPERTY_TOTALWIN, colorTicket * 100 + player.getLong(PLAYER_PROPERTY_TOTALWIN));
			}
			return;
		}
		ServerMsg.PlayerData.Builder build = ServerMsg.PlayerData.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		if (proName.equals(PLAYER_PROPERTY_NAME)) {
			build.setName(player.getString(PLAYER_PROPERTY_NAME));
		}
		if (proName.equals(PLAYER_PROPERTY_SIGN)) {
			build.setSign(player.getString(PLAYER_PROPERTY_SIGN));
		}
		if (proName.equals(PLAYER_PROPERTY_TITLEID)) {
			build.setTitle(player.getString(PLAYER_PROPERTY_TITLEID));
		}
		if (proName.equals(PLAYER_PROPERTY_LEVEL)) {
			build.setLevel(player.getInt(PLAYER_PROPERTY_LEVEL));
		}
		if (proName.equals(PLAYER_PROPERTY_HEADID)) {
			build.setHeadid(player.getInt(PLAYER_PROPERTY_HEADID));
		}
		if (proName.equals(PLAYER_PROPERTY_BULLETLEVEL)) {
			build.setBet(m_BulletValModule.GetMaxBv(player));
		}
		if (proName.equals(PLAYER_PROPERTY_SEX)) {
			build.setSex(player.getInt(PLAYER_PROPERTY_SEX));
		}
		if (proName.equals(PLAYER_PROPERTY_BATTERYINUSE)) {
			build.setSkin(player.getInt(PLAYER_PROPERTY_BATTERYINUSE));
		}
		if (proName.equals(PLAYER_PROPERTY_HITFISHSCORE)) {
			build.setMaxScore(player.getLong(PLAYER_PROPERTY_HITFISHSCORE));
		}
		if (proName.equals(PLAYER_PROPERTY_HITEGGCOUNT)) {
			build.setHitEggCount(player.getInt(PLAYER_PROPERTY_HITEGGCOUNT));
		}
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_PLAYER_DATA.ordinal(), build.build().toByteArray());
	}

	void OnPlayerOnLine(IKernel kernel, IGameObject player) {
		ServerMsg.PlayerData.Builder build = ServerMsg.PlayerData.newBuilder();
		build.setUid(player.getInt(PLAYER_PROPERTY_UID));
		build.setName(player.getString(PLAYER_PROPERTY_NAME));
		build.setSign(player.getString(PLAYER_PROPERTY_SIGN));
		build.setTitle(player.getString(PLAYER_PROPERTY_TITLEID));
		build.setLevel(player.getInt(PLAYER_PROPERTY_LEVEL));
		build.setHeadid(player.getInt(PLAYER_PROPERTY_HEADID));
		build.setSex(player.getInt(PLAYER_PROPERTY_SEX));
		build.setBet(m_BulletValModule.GetMaxBv(player));
		build.setScore(0);
		build.setSkin(player.getInt(PLAYER_PROPERTY_BATTERYINUSE));
		build.setMaxScore(player.getLong(PLAYER_PROPERTY_HITFISHSCORE));
		build.setHitEggCount(player.getInt(PLAYER_PROPERTY_HITEGGCOUNT));
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_PLAYER_DATA.ordinal(), build.build().toByteArray());
		//敏感词替换 add by 赵俊@20190528  换昵称时已经检查 不需要每次玩家登录去检查替换
		player.setProperty(PLAYER_PROPERTY_NAME, replaceSensitiveWord(player.getString(PLAYER_PROPERTY_NAME)));
		//logger.info("热更代码测试成功");
		gameReport(kernel, player.getInt(PLAYER_PROPERTY_UID), 1);
		if (player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME) != 0L
				&& !player.haveHeartBeat(REAL_NAME_AUTH_TIMER)) {
			kernel.addHeartBeat(REAL_NAME_AUTH_TIMER, player, REAL_NAME_AUTH_TIME, -1);
		}
	}

	void OnPlayerOffLine(IKernel kernel, IGameObject player) {
		gameReport(kernel, player.getInt(PLAYER_PROPERTY_UID), 0);
	}

	public void OnRealNameSuccess(IKernel kernel, IGameObject player) {
		if (player.getBool(PLAYER_PROPERTY_REALNAME)) {
			return;
		}
		player.setProperty(PLAYER_PROPERTY_REALNAME,true);
		m_itemModule.AddItem(kernel, player, "item_pkg_realname", 1, UtilFunc.System.REAL_NAME.ordinal(),"OnRealNameSuccess");
		UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, "item_pkg_realname", 1);
	}

	void OnRecvRealName(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		if (player.getBool(PLAYER_PROPERTY_REALNAME)) {
			return;
		}
		CustomMsg.RealName msg = CustomMsg.RealName.parseFrom(data);
		String name = msg.getName();
		String idnum = msg.getIdnum();
		kernel.realName(player, name, idnum, (Boolean res) -> {
			if (res){
				player.setProperty(PLAYER_PROPERTY_IDENTITYCARD,idnum);
				OnRealNameSuccess(kernel,player);
			}
		});
	}

    void OnReqPlayerPlace(IKernel kernel, IGameObject player, int msgid, byte[] data) {
        int currentPlace = player.getInt(PLAYER_CURRENT_PLACE);
        CustomMsg.Int32.Builder build = CustomMsg.Int32.newBuilder();
        build.setValue(currentPlace);
        kernel.sendMessage(player, S2CMsgDef.S2C_PLAYER_PLACE.ordinal(), build.build().toByteArray());
    }


	public void OnBindProxySucc(IKernel kernel, IGameObject player) {
		if (player.getBool(PLAYER_PROPERTY_BINDPROXY)) {
			return;
		}
		player.setProperty(PLAYER_PROPERTY_BINDPROXY, true);
	}

	private void notifyBindProxy(IKernel kernel, IGameObject player, int code) {
		CustomMsg.BindProxyResult.Builder builder = CustomMsg.BindProxyResult.newBuilder();
		builder.setCode(code);
		kernel.sendMessage(player, S2CMsgDef.S2C_BIND_PROXY_OK.ordinal(), builder.build().toByteArray());
	}

	void OnRecvBindProxy(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		if (player.getBool(PLAYER_PROPERTY_BINDPROXY)) {
			notifyBindProxy(kernel, player, 1);
			return;
		}
		CustomMsg.BindProxy msg = CustomMsg.BindProxy.parseFrom(data);
		int proxyId = msg.getProxyId();
		long objid = player.getObjectID();
		kernel.bindProxy(player, proxyId, (res) -> {
			if (!res) {
				notifyBindProxy(kernel, player, 0);
				return;
			}
			if (kernel.getGameObject(objid) == player) {
				OnBindProxySucc(kernel, player);
				player.setProperty(PLAYER_PROPERTY_PROPERTY_PROXY_ID, proxyId);
				notifyBindProxy(kernel, player, 1);
			} else {
				m_OfflineDataModule.AddOfflineData(kernel, player.getInt(PLAYER_PROPERTY_UID), OfflineDataType.BIND_PROXY, "", "");
			}
		});
	}

	void OnRecvSetOpt(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.LastOption opt = CustomMsg.LastOption.parseFrom(data);
		int type = opt.getType();
		String value = opt.getValue();
		SetLastOpt(player, type, value);
	}

	/*
	void OnRecvSetHeadId(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.Int32 msg = CustomMsg.Int32.parseFrom(data);
		player.setProperty(PLAYER_PROPERTY_HEADID, msg.getValue());
	}
	 */

	void OnRecvFirstTip(IKernel kernel, IGameObject player, int msgid, byte[] data) {
		player.setProperty(PLAYER_PROPERTY_FIRSTINROOM, 0);
	}

	public void SetLastOpt(IGameObject player, int type, String opt) {
		if (type <= OptType.TYPE_UNKNOW.ordinal() || type >= OptType.TYPE_END.ordinal()) {
			return;
		}

		String lastOpt = type + "," + opt;
		player.setProperty(PLAYER_PROPERTY_LASTOPT, lastOpt);
	}

	void OnRecvSetSign(IKernel kernel, IGameObject player, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.String val = CustomMsg.String.parseFrom(data);
		player.setProperty(PLAYER_PROPERTY_SIGN, val.getValue());
	}

	private OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
			.writeTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

	public class BindCallBack implements Callback {
		private IKernel kernel;
		private long player;
		private int uid;

		public BindCallBack(IKernel kernel, int uid, long player) {
			this.kernel = kernel;
			this.uid = uid;
			this.player = player;
		}

		@Override
		public void onFailure(Call call, IOException e) {
			logger.error("BindCallBack onFailure: {}", e);
		}

		@Override
		public void onResponse(Call call, Response response) throws IOException {
			String respBody = response.body().string();
			logger.error("BindCallBack onResponse: {}", respBody);
			OnBindRes(kernel, player, uid, respBody);
		}
	}

	public class ChangePWCallBack implements OnSuccess<String>,OnError {
		private IKernel kernel;
		private long player;
		private int uid;

		public ChangePWCallBack(IKernel kernel, int uid, long player) {
			this.kernel = kernel;
			this.uid = uid;
			this.player = player;
		}

		@Override
		public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
			logger.error("ChangePWCallBack onError: {}", ex);
		}

		@Override
		public void onSuccess(String data, ForestRequest request, ForestResponse response) {
			logger.error("ChangePWCallBack OnSuccess: {}", data);
			OnChangePWRes(kernel, player, uid, data);
		}
	}

	void OnChangePWRes(IKernel kernel, long objid, int uid, String res) {
		IGameObject player = kernel.getGameObject(objid);
		if (player == null) {
			// offline
			m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.CHANGE_PASSWORD, res, "");
		} else {
			OnChangePWRes(kernel, player, res);
		}
	}

	public void OnChangePWRes(IKernel kernel, IGameObject player, String res) {
		JsonObject json = JsonUtil.decodeToObj(res,JsonObject.class);
		int code = json.get("result").getAsInt();
		if (player.haveTempData("ChangePasswordReqID")) {
			int reqId = player.getTempInt("ChangePasswordReqID");
			CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
			build.setCode(code);
			kernel.response(player,reqId,build.build().toByteArray());
			player.removeTempData("ChangePasswordReqID");
		}
	}

	public class BindPhoneCallBack implements OnSuccess<String>,OnError {
		private IKernel kernel;
		private long player;
		private int uid;
		private String phone;

		public BindPhoneCallBack(IKernel kernel, int uid, long player, String phone) {
			this.kernel = kernel;
			this.uid = uid;
			this.player = player;
			this.phone = phone;
		}


		@Override
		public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
			logger.error("BindPhoneCallBack onError: {}", ex);
		}

		@Override
		public void onSuccess(String data, ForestRequest request, ForestResponse response) {
			logger.info("BindPhoneCallBack onSuccess: {}", data);
			OnBindPhoneRes(kernel, player, uid,data,phone);
		}
	}

	void OnBindPhoneRes(IKernel kernel, long objid, int uid, String res, String phone) {
		IGameObject player = kernel.getGameObject(objid);
		String content = res + "-" + phone;
		JsonObject json = JsonUtil.decodeToObj(res, JsonObject.class);
		if (json.get("result").getAsInt() == 0) {
			player.setProperty(PLAYER_PROPERTY_BINDPHONE_BEFORE, 1);
		}
		if (player == null) {
			// offline
			m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.BIND_PHONE, content, "");
		} else {
			OnBindPhoneRes(kernel, player, content);
		}
	}

	public void OnBindPhoneRes(IKernel kernel, IGameObject player, String content) {
		String[] contents = content.split("-");
		JsonObject json = JsonUtil.decodeToObj(contents[0],JsonObject.class);
		int code = json.get("result").getAsInt();
		if (code == 0) {
			player.setProperty(PLAYER_PROPERTY_PHONE, contents[1]);
		}
		if (player.haveTempData("BindPhoneReqID")) {
			int reqid = player.getTempInt("BindPhoneReqID");
			CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
			build.setCode(code);
			kernel.response(player, reqid, build.build().toByteArray());
			player.removeTempData("BindPhoneReqID");
		}
	}

	public class UnBindPhoneCallBack implements OnSuccess<String>,OnError {
		private IKernel kernel;
		private long player;
		private int uid;

		public UnBindPhoneCallBack(IKernel kernel, int uid, long player) {
			this.kernel = kernel;
			this.uid = uid;
			this.player = player;
		}

		@Override
		public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
			logger.error("UnBindPhoneCallBack onError: {}", ex);
		}

		@Override
		public void onSuccess(String data, ForestRequest request, ForestResponse response) {
			logger.error("UnBindPhoneCallBack onSuccess: {}", data);
			OnUnBindPhoneRes(kernel, player, uid, data);
		}
	}

	void OnUnBindPhoneRes(IKernel kernel, long objid, int uid, String res) {
		IGameObject player = kernel.getGameObject(objid);
		if (player == null) {
			// offline
			m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.UN_BIND_PHONE, res, "");
		} else {
			OnUnBindPhoneRes(kernel, player, res);
		}
	}

	public void OnUnBindPhoneRes(IKernel kernel, IGameObject player, String res) {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(res).getAsJsonObject();
		int code = json.get("result").getAsInt();
		if (code == 0) {
			player.setProperty(PLAYER_PROPERTY_PHONE, json.get("phone").getAsString());
		}
		if (player.haveTempData("UnBindPhoneReqID")) {
			int reqid = player.getTempInt("UnBindPhoneReqID");
			CustomMsg.ServerCode.Builder build = CustomMsg.ServerCode.newBuilder();
			build.setCode(code);
			kernel.response(player, reqid, build.build().toByteArray());
			player.removeTempData("UnBindPhoneReqID");
		}
	}

	void OnBindRes(IKernel kernel, long objid, int uid, String res) {
		IGameObject player = kernel.getGameObject(objid);
		if (player == null) {
			// offline
			m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.BIND_CHANNEL, res, "");
		} else {
			OnBindRes(kernel, player, res);
		}
	}

	public void OnBindRes(IKernel kernel, IGameObject player, String res) {
		ServerCodeDef code = ServerCodeDef.CODE_FAILED;
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(res).getAsJsonObject();
		if (json.get("code").getAsInt() == 0) {
			code = ServerCodeDef.CODE_SUCCESS;
			player.setProperty(PLAYER_PROPERTY_BINDCHANNEL, json.get("sdkId").getAsString());
			player.setProperty(PLAYER_PROPERTY_PAYINFO, json.get("payInfo").getAsString());
		}

		if (player.haveTempData("BindReqID")) {
			int reqid = player.getTempInt("BindReqID");
			UtilFunc.responseSerCode(kernel, player, reqid, code);
			player.removeTempData("BindReqID");
		}
	}

	void OnReqRoomPlayer(IKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.IntSingle msg = ServerMsg.IntSingle.parseFrom(data);
		int type = msg.getIntMember();

		ServerMsg.RoomPlayerData.Builder build = ServerMsg.RoomPlayerData.newBuilder();
		do {
			IGameObject room = kernel.getPreloadObject("room" + type);
			if (room == null) {
				break;
			}

			List<Long> desks = new ArrayList<>();
			room.getChildsByType(GameObjectType.GOTYPE_DESK, desks);
			for (long deskid : desks) {
				IGameObject desk = kernel.getGameObject(deskid);
				int count = desk.getSeatCount();
				for (int i = 0; i < count; ++i) {
					IGameObject obj = desk.getSeatObject(i);
					if (obj == null || obj.getBool("IsRobot")) {
						continue;
					}

					build.addUid(obj.getInt(PLAYER_PROPERTY_UID));
				}
			}
		} while (false);

		kernel.responseServer(reqid, build.build().toByteArray());
	}

	void OnRealNameSuccess(IKernel kernel, long playerId, int uid, String name, String idnum, int code) {
		IGameObject player = kernel.getGameObject(playerId);
		StringBuilder builder = new StringBuilder();
		builder.append(code).append("_").append(name).append("_").append(idnum);
		if (player == null) {
			m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.REAL_NAME_SUCCESS, builder.toString(), "");
		} else {
			OnRealNameSuccess(kernel, player, builder.toString());
		}
	}

	public void OnRealNameSuccess(IKernel kernel, IGameObject player, String result){
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)) {
			return;
		}
		String[] tmp = result.split("_");
		if (tmp.length < 3) {
			notifyRealNameResult(kernel, player, -1);
			return;
		}
		String tmpName = tmp[1];
		String tmpId = tmp[2];
		String name = tmpName.charAt(0) + "**";
		String idNum = tmpId.substring(0, 14) + "****";
		int code = Integer.parseInt(tmp[0]);
		if (code != 0) {
			// 认证中的状态
			if (code == -4) {
				if (!player.haveHeartBeat(REAL_NAME_AUTH_TIMER)) {
					player.setTempData("RealNameAuth_Name", tmpName);
					player.setTempData("RealNameAuth_idNum", tmpId);
					player.setProperty(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME, kernel.getServerTime() + REAL_NAME_AUTH_OVER_TIME);
					kernel.addHeartBeat(REAL_NAME_AUTH_TIMER, player, REAL_NAME_AUTH_TIME, -1);
				} else if (!tmpName.equals(player.getTempString("RealNameAuth_Name"))
						|| !tmpId.equals(player.getTempString("RealNameAuth_idNum"))) {
					// 实名认证信息改变 心跳重置
					player.removeHeartBeat(REAL_NAME_AUTH_TIMER);
					player.setTempData("RealNameAuth_Name", tmpName);
					player.setTempData("RealNameAuth_idNum", tmpId);
					player.setProperty(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME, kernel.getServerTime() + REAL_NAME_AUTH_OVER_TIME);
					kernel.addHeartBeat(REAL_NAME_AUTH_TIMER, player, REAL_NAME_AUTH_TIME, -1);
				}
			}
			notifyRealNameResult(kernel, player, code);
			return;
		}
		kernel.realName(player, name, idNum, (Boolean res) -> {
			if (res) {
				player.setProperty(PLAYER_PROPERTY_IDENTITYCARD, idNum);
				if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)) {
					return;
				}
				player.setProperty(PLAYER_PROPERTY_CERTIFICATION, true);
				m_itemModule.AddItem(kernel, player, "item_pkg_realname", 1, UtilFunc.System.REAL_NAME.ordinal(), "OnRealNameSuccess");
				notifyRealNameResult(kernel, player, code);
				CustomMsg.Int32.Builder build = CustomMsg.Int32.newBuilder();
				build.setValue(0);
				kernel.sendMessage(player, S2CMsgDef.S2C_REAL_NAME_AUTH.ordinal(), build.build().toByteArray());
				UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, "item_pkg_realname", 1);
				player.setProperty(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME, 0L);
			}
		});
	}

	void notifyRealNameResult(IKernel kernel, IGameObject player, int code) {
		if (player.haveTempData("RealNameAuthReqID")) {
			int reqid = player.getTempInt("RealNameAuthReqID");
			CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
			Map<String, Object> map = new HashMap<>();
			map.put("code", code);
			map.put("endTime", player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME));
			builder.setValue(JsonUtil.encodeToStr(map));
			kernel.response(player, reqid, builder.build().toByteArray());
			player.removeTempData("RealNameAuthReqID");
		}
	}

	public class RealNameCallBack implements OnSuccess<String>,OnError {
		private IKernel kernel;
		private long player;
		private int uid;
		private String name;
		private String idNum;

		public RealNameCallBack(IKernel kernel, int uid, long player, String name, String idNum) {
			this.kernel = kernel;
			this.uid = uid;
			this.player = player;
			this.name = name;
			this.idNum = idNum;
		}

		@Override
		public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
			logger.error("RealNameCallBack onError: {}", ex);
		}

		@Override
		public void onSuccess(String data, ForestRequest request, ForestResponse response) {
			logger.info("RealNameCallBack onSuccess: {}", data);
			JsonParser parser = new JsonParser();
			JsonObject json = parser.parse(data).getAsJsonObject();
			int code = json.get("code").getAsInt();
			OnRealNameSuccess(kernel, player, uid,  name, idNum, code);
		}
	}

	// 请求实名认证
	public void OnRecvRealName(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		if (player.getBool(PLAYER_PROPERTY_CERTIFICATION)) {
			return;
		}
		CustomMsg.RealName msg = CustomMsg.RealName.parseFrom(data);
		String name = msg.getName();
		String idNum = msg.getIdnum();
		if (name.length() < 1 || idNum.length() < 18) {
			notifyRealNameResult(kernel, player, -1);
			return;
		}
		String url = SystemConfigData.getLoginServerUrl("realNameAuth");
		player.addTempData("RealNameAuthReqID", ValueType.INT, reqid);
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid", uid);
		map.put("userName", name);
		map.put("idNum", idNum);
		RealNameCallBack callBack = new RealNameCallBack(kernel,uid, player.getObjectID(), name, idNum);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}

	public void OnRealNameAuthTime(IKernel kernel, IGameObject player) {
		if (!player.haveTempData("RealNameAuth_Name") || !player.haveTempData("RealNameAuth_idNum")
				|| player.getBool(PLAYER_PROPERTY_CERTIFICATION)) {
			kernel.removeHeartBeat(player, REAL_NAME_AUTH_TIMER);
			return;
		}
		if (kernel.getServerTime() > player.getLong(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME)) {
			player.removeTempData("RealNameAuth_Name");
			player.removeTempData("RealNameAuth_idNum");
			player.setProperty(PLAYER_PROPERTY_SYS_REAL_NAME_AUTH_END_TIME, 0L);
			kernel.removeHeartBeat(player, REAL_NAME_AUTH_TIMER);
			CustomMsg.Int32.Builder build = CustomMsg.Int32.newBuilder();
			build.setValue(1);
			kernel.sendMessage(player, S2CMsgDef.S2C_REAL_NAME_AUTH.ordinal(), build.build().toByteArray());
			return;
		}
		String name  = player.getTempString("RealNameAuth_Name");
		String idNum = player.getTempString("RealNameAuth_idNum");
		String url = SystemConfigData.getLoginServerUrl("realNameAuth");
		Map<String,Object> map = new HashMap<>();
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		map.put("uid", uid);
		map.put("userName", name);
		map.put("idNum", idNum);
		RealNameCallBack callBack = new RealNameCallBack(kernel,uid, player.getObjectID(), name, idNum);
		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,callBack,callBack);
	}


	/**
	 * 上报游戏 废弃20230403
	 * @param kernel
	 * @param uid 玩家uid
	 * @param op 0下线 1上线
	 */
	void gameReport(IKernel kernel, int uid, int op) {
//		String url = SystemConfigData.getLoginServerUrl("gameReport");
//		Map<String,Object> map = new HashMap<>();
//		map.put("uid", uid);
//		map.put("op", op);
//		HttpClientUtil.doPost(kernel.getHttpClient(),url,map,(data,request,response)->{
//			logger.info("GameReportCallback OnSuccess: {}", data);
//		},(ex, request,response)->{
//			logger.error("GameReportCallback OnError: {}", ex);
//		});
	}

	public class GameReportCallback implements Callback {
		private IKernel kernel;
		private int uid;

		public GameReportCallback(IKernel kernel, int uid) {
			this.kernel = kernel;
			this.uid = uid;
		}

		@Override
		public void onFailure(Call call, IOException e) {
			logger.error("GameReportCallback onFailure: {}", e);
		}

		@Override
		public void onResponse(Call call, Response response) throws IOException {
			String respBody = response.body().string();
//			logger.info("GameReportCallback onResponse: uid:{}, resp:{}",uid, respBody);
		}
	}

	// 查询玩家总赢
	void onGetPlayerTotalWin(IKernel kernel, int reqid, byte[] data){
		int uid = Integer.parseInt(new String(data));
		IGameObject player = kernel.getPlayer(uid);
		IRecord iRecord = player.getRecord("TotalPlayWin");
		long totalWin = 0L;
		for (int i = 0; i < RoomType.ROOM_END.ordinal(); i++) {
			totalWin += iRecord.getLong(i, 1);
		}
		kernel.responseServer(reqid, String.valueOf(totalWin).getBytes());
	}

	public void OnGetOnlinePlayerBombCount(IKernel kernel, int msgid, byte[] data) throws Exception {
		ServerMsg.IntList list = ServerMsg.IntList.parseFrom(data);
		// uid集合
		List<Integer> uidList = list.getElementList();
		List<GamePlayer> gamePlayerList = kernel.listPlayer(uidList);
		// 初始化
		IoBuffer buffer = IoBuffer.allocate(1024);
		// 开启自动扩容
		buffer.setAutoExpand(true);
		if (CollectionUtils.isEmpty(gamePlayerList)) {
			buffer.putLong(0l);
			buffer.putLong(0l);
			logger.info("没有在线玩家开天斧数量  {}  魔晶数量   {}",0,0);
			buffer.flip();
			kernel.responseServer(msgid, buffer.array());
			return;
		}
		long sum = kernel.getAllOnlinePlayer().stream().mapToLong(a -> a.getInt(PropertyKey.PLAYER_PROPERTY_BOMB_ITEM)).sum();
		long bomb = kernel.getAllOnlinePlayer().stream().mapToLong(a -> a.getLong(PropertyKey.PLAYER_PROPERTY_BOMB_COIN)).sum();
		buffer.putLong(bomb);
		buffer.putLong(sum);
		logger.info("在线玩家开天斧数量  {}  魔晶数量   {}",sum,bomb);
		// 转为写出模式
		buffer.flip();
		kernel.responseServer(msgid, buffer.array());
	}
	public void OnGetOnlinePlayerList(IKernel kernel, int msgid, byte[] data) throws Exception {
		ServerMsg.IntList list = ServerMsg.IntList.parseFrom(data);
		// uid集合
		List<Integer> uidList = list.getElementList();
		List<GamePlayer> gamePlayerList = kernel.listPlayer(uidList);
		// 初始化
		IoBuffer buffer = IoBuffer.allocate(1024);
		// 开启自动扩容
		buffer.setAutoExpand(true);
		if (CollectionUtils.isEmpty(gamePlayerList)) {
			buffer.putInt(0);
			kernel.responseServer(msgid, buffer.array());
			return;
		}
		// 玩家对象总数
		buffer.putInt(gamePlayerList.size());

		for (GamePlayer player : gamePlayerList) {

			OnlinePlayer onlinePlayer = new OnlinePlayer();
			int uid = player.getInt(PLAYER_PROPERTY_UID);
			String name = player.getString(PLAYER_PROPERTY_NAME);
			int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
			int level = player.getInt(PLAYER_PROPERTY_LEVEL);
			int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
			long gold = player.getLong(PLAYER_PROPERTY_GOLD);
			long bombCoin = player.getLong(PLAYER_PROPERTY_BOMB_COIN);
			long diamond = player.getLong(PLAYER_PROPERTY_DIAMOND);
			long colorTicket = player.getLong(PLAYER_PROPERTY_COLORTICKET);
//			long consumedColorTicket = player.GetLong(PLAYER_PROPERTY_TOTALUSECOLORTICKET);
			int bulletLevel = player.getInt(PLAYER_PROPERTY_BULLETLEVEL);
			int nBulletLevel = player.getInt(PLAYER_PROPERTY_NBULLETLEVEL);
			int rechargeScore = player.getInt(PLAYER_PROPERTY_RECHARGESCORE);
			int itemScore = player.getInt(PLAYER_PROPERTY_ITEMSCORE);
			long shutUp = player.getLong(PLAYER_PROPERTY_SHUTUP);
			int totalRecharge = player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT);
			long deskId = player.getLong(PLAYER_PROPERTY_DESKID);
//			IGameObject deskGO = kernel.GetGameObject(deskId);
//			int online = -1;
//			if (deskGO != null) {
//				online = deskGO.GetInt("Type");
//			}
			IGameObject itemBag = player.getContainer("ItemBag");
			StringBuilder prop = new StringBuilder();
			if (itemBag != null) {

				for (int i = 0; i < itemBag.getCapacity(); ++i) {
					IGameObject item = itemBag.getChild(i);
					if (item == null) continue;
					String itemId = item.getString("Id");
					int count = item.getInt("Count");
					prop.append(itemId).append(":").append(count).append(" ");
				}
			}
			prop = new StringBuilder(prop.toString().trim());

			onlinePlayer.setUid(uid);
			onlinePlayer.setNickname(name);
			onlinePlayer.setChannel(channel);
			onlinePlayer.setLevel(level);
			onlinePlayer.setVipLevel(vipLevel);
			onlinePlayer.setGold(gold);
			onlinePlayer.setDiamond(diamond);
			onlinePlayer.setColorTicket(colorTicket);
			onlinePlayer.setItem(prop.toString());
			onlinePlayer.setTotalRecharge(totalRecharge);
			onlinePlayer.setBulletLevel(bulletLevel);
			onlinePlayer.setNuclearBulletLevel(nBulletLevel);
			onlinePlayer.setBombCoin(bombCoin);
			onlinePlayer.setRechargeScore(rechargeScore);
			onlinePlayer.setItemScore(itemScore);
//			onlinePlayer.setShutUp(shutUp);
//			onlinePlayer.setStatus(0);
//			onlinePlayer.setOnline(online);
//			onlinePlayer.setBank("ICBC");
//			onlinePlayer.setTempCellphone(player.GetString(StoreModule.PROPERTY_LAST_TEMP_CELLPHONE));

			// 对象转为byte数组
			byte[] bytes = ByteUtils.objectToByte(onlinePlayer);
			// 此对象的byte数组长度
			buffer.putInt(bytes.length);
			// 此对象的byte数组
			buffer.put(bytes);
		}

		// 转为写出模式
		buffer.flip();
		kernel.responseServer(msgid, buffer.array());
	}

	void OnReqDeductItem(IKernel kernel, int reqId, byte[] data) throws Exception {
		DeductItemGameDTO gameDTO = ByteUtils.byteToObject(data);
		IGameObject player = kernel.getPlayer(gameDTO.getUid());
		UtilFunc.System system = UtilFunc.System.BACK_DEDUCT_ITEM;
		m_itemModule.SubItem(kernel, player, gameDTO.getItemId(), gameDTO.getCount(), system.ordinal(), system.getLabel());
		// 魔晶预警-后台扣除道具
		kernel.responseServer(reqId, new byte[0]);
	}
	public void StorePlayerRedisPubInfor(IKernel kernel,IGameObject player){
		Jedis jedis = kernel.getJedis();
		JSONObject jsonObject=new JSONObject();
		jsonObject.put(PLAYER_PROPERTY_UID,player.getInt(PLAYER_PROPERTY_UID));
		jsonObject.put(PLAYER_PROPERTY_LEVEL,player.getInt(PLAYER_PROPERTY_LEVEL));
		jsonObject.put(PLAYER_PROPERTY_HEADID,player.getInt(PropertyKey.PLAYER_PROPERTY_HEADID));
		jsonObject.put(PLAYER_PROPERTY_HEAD,player.getString(PropertyKey.PLAYER_PROPERTY_HEAD));
		jsonObject.put(PLAYER_PROPERTY_NAME,player.getString(PropertyKey.PLAYER_PROPERTY_NAME));
		jedis.set(PLAYER_REDIS_PUB_INFO+"::"+player.getProperty(PLAYER_PROPERTY_UID).toString(),jsonObject.toJSONString());
	}
	public JSONObject getPlayerRedisPubInfo(IKernel kernel,String  UID){
		Jedis jedis = kernel.getJedis();
		String value = jedis.get(PLAYER_REDIS_PUB_INFO + "::" + UID);
		return JSONObject.parseObject(value);
	}

}
