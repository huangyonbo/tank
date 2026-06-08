package game.modules;

import back.modules.dataenum.RoomType;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.JsonUtil;
import framework.game.*;
import framework.mybatis.domain.MojinRoomRecord;
import framework.mybatis.domain.PlayerDailyPlayData;
import framework.pub.IPubData;
import framework.pub.IPubRecord;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.fishgame.BulletValModule;
import game.modules.fishgame.FishModule;
import game.modules.fishgame.FishModule.FishData;
import game.modules.utils.UtilFunc;
import game.syslog.LogManager;
import game.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * 描述： 房间模块 创建人：胡中伟 创建时间：2018年3月27日 下午3:02:06
 * 
 */
public class RoomModule implements ILogicModule {
	public enum DeskColType {
		COL_DESKID, COL_DESK_SEAT_COUNT, COL_DESK_PLAYER_COUNT,

		COL_MAX
	}

	public enum MojinRoomRecordColType {
		COL_ROOMID, COL_ENTER_TIMESTAMP,

		COL_TOTAL_PLAY, COL_TOTAL_COST, COL_ISATCIVE
	}

	public enum NuclearRoomPlayRecordCol {
		COL_ROOM_ID, COL_ROOM_DF, COL_TODAY_PLAY_TIME, COL_TOTAL_PLAY, COL_TOTAL_WIN, COL_ENTER_TIMES, COL_RECORD_DATE,

		COL_END
	}

	private static Logger logger = LoggerFactory.getLogger(RoomModule.class);
	private TimerManager m_TimerManager;
	private FishModule m_FishModule;
	private BulletValModule m_BulletValModule;
	private boolean needUpdate = false; // 是否需要刷新房间出鱼信息
	String PLAYER_ROOM_BIGKUN_GOLD_POOL = "BigKunGoldPool";//巨鲲活动奖励池


	List<String> m_roomList = new ArrayList<>();
	String[] fishPkgKind = { "SmallPkg", "TeamPkg", "NormalPkg", "BigPkg", "ColorPkg", "GroupPkg", "BombPkg", "FuncPkg",
			"SpecPkg", "BossPkg", "WorldBoss", "ThunderDragon" };

	public RoomModule(IKernel kernel) {
		kernel.addClass("FishRoom", "Room");
		kernel.addClass("ArenaRoom", "FishRoom");
		kernel.addClass("BossRoom", "FishRoom");
	}
	public enum PoolRecColType{
		PLAYER_UID,
		PLAYER_VIP,
		GET_RATIO,
		GET_GOLD,
		END
	}
	class DragonBv {
		int id;
		int min;
		int max;
	}

	class FishInfo {
		int id;
		String name;
		int minBet;
		int maxBet;
		int type;
		String describe;
		float speed;

		public int getMaxBet() {
			return maxBet;
		}

	}
	
	class GameDetail {
		long enterTime;
		long totalPlay;
	}

	public static class GoldRoomConfig{
		public Integer roomId;
		public Integer vipLimit;
		public Integer minBv;
		public Integer maxBv;
		public Long maxGold;
	}
	
	Map<Integer, DragonBv> m_mapDragonBvCfg = new HashMap<>();
	Map<Integer, List<String>> m_mapRoomPkgList = new HashMap<>(); // 保存这个房间出哪些鱼pkg
	Map<Integer, List<FishInfo>> m_mapRoomFishList = new HashMap<>(); // 保存这个房间出哪些具体的鱼
	Map<String, List<Integer>> m_mapRandPkg = new HashMap<>(); // pkg里有哪些鱼
	Map<Integer, String> m_mapFormation = new HashMap<>(); // 房间出哪些鱼阵
	Map<Integer, GoldRoomConfig> goldRoomConfigMap = new HashMap<>(); // 金币场配置参数
	
	Map<Integer, List<Integer>> m_mRoomPlayers = new HashMap<>(); // 房间成员列表(只要当天进入就记上)
	Map<Integer, Map<Integer, GameDetail>> m_mPlayerGameDetail = new HashMap<>(); // 每个房间(魔晶场)的玩家游玩片段
	List<Integer> activePlayerList = new ArrayList<>();  // 日活跃用户玩家
	private static final String NUCLEAR_ROOM_PLAY_RECORD = "nuclearRoomPlayRecordLast";

	public Map<Integer, GoldRoomConfig> getGoldRoomConfigMap() {
		return goldRoomConfigMap;
	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "FishRoom", this, "OnRoomClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "FishRoom", this, "OnRoomLoad");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_SITDOWN, "Player", this, "OnPlayerSitDown");
		kernel.regEvent(KernelEvent.KEVENT_ON_STANDUP, "Player", this, "OnPlayerStandUp");
		kernel.regEvent(KernelEvent.KEVENT_ON_DESTROY, "FishDesk", this, "OnDeskDestroy");
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE, "World", this, "OnWorldCreate");

		kernel.regServerMsg(ServerMsgDef.MMSG_CREATE_DESK.ordinal(), this, "OnRecvCreateDesk");
		kernel.regServerMsg(ServerMsgDef.M2G_CREATE_MYSTERY_LEGEND_DESK.ordinal(), this, "OnCreateMysteryLegendDesk");
		kernel.regServerMsg(ServerMsgDef.P2G_ALLROOM_RUNINFO.ordinal(), this, "OnRecvAllRoomRunInfo");

		kernel.regRequestMessage(RequestMsgDef.REQ_ROOM_PLAYERS.ordinal(), this, "OnReqRoomPlayers");
		kernel.regRequestMessage(RequestMsgDef.REQ_ROOM_FISHLIST.ordinal(), this, "OnReqRoomFishList");
		kernel.regRequestMessage(RequestMsgDef.REQ_GOLD_ROOM_CONFIG.ordinal(), this, "OnReqGoldRoomConfig");
		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		kernel.regCommand(CommandDef.CMD_CHANGE_DAY.ordinal(), "Player", this, "OnCmdChangeDay");

		kernel.regStopListener(this, UtilFunc.StopListenerOrder.ROOM_MODULE.ordinal(), "StoreMojinRoomRecord");

		kernel.preLoadConfig("res/Game/Room.xml");
		RefreshCfg(kernel, "res/Game/FireDragon.xml");

		ICfgReader cfg = kernel.loadXmlConfig("res/Game/Room.xml");
		if (cfg == null) {
			return false;
		}

		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			String id = cfg.getString(i, "Id");
			m_roomList.add(id);
			kernel.preLoadObject(id);
		}
		RefreshCfg(kernel, "res/Game/Room.xml");
		RefreshCfg(kernel, "res/RandPkg/RandPkg.xml");

		m_TimerManager = (TimerManager) kernel.getModule("TimerManager");
		m_FishModule = (FishModule) kernel.getModule("FishModule");
		m_BulletValModule = (BulletValModule) kernel.getModule("BulletValModule");

		m_TimerManager.addChangeDayCallBack(this, "OnChangeDay");
		
		return true;
	}

	void RefreshCfg(IKernel kernel, String path) {
		if (path.equals("res/Game/FireDragon.xml")) {
			ICfgReader cfg = kernel.loadXmlConfig(path);
			if (cfg == null) {
				return;
			}
			int count = cfg.getItemCount();
			for (int i = 0; i < count; ++i) {
				DragonBv bv = new DragonBv();
				bv.id = cfg.getInt(i, "Id");
				bv.min = cfg.getInt(i, "Min");
				bv.max = cfg.getInt(i, "Max");
				m_mapDragonBvCfg.put(bv.id, bv);
			}
		}

		// 初始化或热更新 m_mapRandPkg
		if (path.equals("res/RandPkg/RandPkg.xml")) {
			ICfgReader mcfg = kernel.loadXmlConfig(path);
			if (mcfg == null) {
				return;
			}
			m_mapRandPkg.clear();
			int count = mcfg.getItemCount();
			for (int k = 0; k < count; ++k) {
				String str = mcfg.getString(k, "Rand");
				if (str == null || str.length() == 0 || !str.startsWith("fish")) {
					continue;
				}
				List<Integer> m_list = new ArrayList<>();
				String[] tmp = str.split(";");
				for (int r = 0; r < tmp.length; ++r) {
					m_list.add(Integer.parseInt(tmp[r].split("\\*")[0].split("fish")[1]));
				}
				m_mapRandPkg.put(mcfg.getString(k, "Id"), m_list);
			}
		}

		// 初始化或更新房间会有哪些出鱼pkg和鱼阵配置
		if (path.equals("res/Game/Room.xml")) {
			ICfgReader cfg = kernel.loadXmlConfig(path);
			if (cfg == null) {
				return;
			}

			m_mapRoomPkgList.clear();
			m_mapFormation.clear();
			goldRoomConfigMap.clear();

			int count = cfg.getItemCount();
			for (int i = 0; i < count; ++i) {
				int id = Integer.parseInt(cfg.getString(i, "Id").split("room")[1]);

				if (RoomModule.isClassic(id)){
					GoldRoomConfig goldRoomConfig = new GoldRoomConfig();
					goldRoomConfig.roomId = id;
					goldRoomConfig.minBv = cfg.getInt(i, "MinBV");
					goldRoomConfig.maxBv = cfg.getInt(i, "MaxBV");
					goldRoomConfig.vipLimit = cfg.getInt(i, "VipLimit");
					goldRoomConfig.maxGold = cfg.getLong(i, "MaxGold");
					goldRoomConfigMap.put(id, goldRoomConfig);
				}

				List<String> m_pkgList = new ArrayList<>();
				for (int j = 0; j < fishPkgKind.length; ++j) {
					m_pkgList.add(cfg.getString(i, fishPkgKind[j]));
				}
				m_mapRoomPkgList.put(id, m_pkgList);

				// 房间有哪些鱼阵
				String m_formation = cfg.getString(i, "Formation");
				if (m_formation == null || m_formation.length() == 0) {
					continue;
				}
				m_mapFormation.put(id, m_formation);
			}
		}
		if (path.contains("res/Formation") || path.equals("res/Game/Room.xml") || path.equals("res/RandPkg/RandPkg.xml")
				|| path.equals("res/Game/Fish.xml")) {
			needUpdate = true;
		}
	}

	@Override
	public void onNetReady(IKernel kernel) {
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_INIT_PLAYER_COUNT.ordinal(), new byte[] {});
	}

	// 更新房间会出哪些鱼
	void updateRoomFishList(IKernel kernel) {

	}

	public void OnWorldCreate(IKernel kernel, IGameObject world) {
		//kernel.SendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_INIT_PLAYER_COUNT.ordinal(), new byte[] {});
	}

	@Override
	public void onDestroy() {

	}

	public void OnRoomClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_ROOM_BIGKUN_GOLD_POOL, ValueType.INT, true, true, true);//巨鲲的奖励池
		//巨鲲奖励池排行榜
		IRecord poolRec = kernel.declareRecord(script, "KunGoldPoolRec", PoolRecColType.END.ordinal(), 10, true, true, true);
		poolRec.setColType(PoolRecColType.PLAYER_UID.ordinal(),ValueType.INT);
		poolRec.setColType(PoolRecColType.PLAYER_VIP.ordinal(),ValueType.INT);
		poolRec.setColType(PoolRecColType.GET_RATIO.ordinal(),ValueType.DOUBLE);
		poolRec.setColType(PoolRecColType.GET_GOLD.ordinal(),ValueType.INT);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALPLAY, ValueType.LONG, false, false, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALWIN, ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "TodayPlay", ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "TodayWin", ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "Type", ValueType.INT, false, false, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_DESKID, ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "Boss", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "SmallPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "TeamPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "NormalPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "BigPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "ColorPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "GroupPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "BombPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "FuncPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "SpecPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "BossPkg", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "Formation", ValueType.OBJECT, false, false, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_OFFPROTECT, ValueType.INT, false, false, false);
		kernel.declareProperty(script, "MinBV", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "MaxBV", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "VipLimit", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "AutoKick", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "AllowQuickEnter", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "WorldBoss", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "ThunderDragon", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "MaxGold", ValueType.LONG, false, false, false);

		kernel.declareProperty(script, "TodayTotalPlayerCount", ValueType.INT, false, false, false); // 游玩总人次
		kernel.declareProperty(script, "TodayActivePlayer", ValueType.INT, false, false, false); // 日活跃用户
		kernel.declareProperty(script, "TodayTotalPlayTime", ValueType.LONG, false, false, false); // 总游玩时长
		kernel.declareProperty(script, "TodayTotalEnterTimes", ValueType.INT, false, false, false); // 总进入次数
		kernel.declareProperty(script, "CurPlayerCount", ValueType.INT, false, false, false); // 当前游玩人数
		kernel.declareProperty(script, "MaxPlayerCount", ValueType.INT, false, false, false); // 同时游玩峰值
		kernel.declareProperty(script, "RoomDf", ValueType.INT, false, false, false); // 魔晶场房间难度

		//C++算法库调用 Add by 谭勇 20211129
		kernel.declareProperty(script,ROOM_PROPERTY_SF_COUNT,ValueType.LONG,false, false, false);//房间算法调用计数器
		kernel.declareProperty(script,ROOM_PROPERTY_SF_VALUE,ValueType.DOUBLE,false, false, false);//房间算法难度调控值

        kernel.declareProperty(script, ROOM_PROPERTY_FUNCTION_FISH_HIT_NUM, ValueType.LONG, false, false, false);

		IRecord deskRec = kernel.declareRecord(script, "DeskRec", DeskColType.COL_MAX.ordinal(), 100, false, false, false);
		deskRec.setColType(DeskColType.COL_DESKID.ordinal(), ValueType.LONG);
		deskRec.setColType(DeskColType.COL_DESK_SEAT_COUNT.ordinal(), ValueType.INT);
		deskRec.setColType(DeskColType.COL_DESK_PLAYER_COUNT.ordinal(), ValueType.INT);
		
	}

	public static boolean isSupreme(int type) {
		return (type >= RoomType.ROOM_SUPREME_1.ordinal() && type <= RoomType.ROOM_SUPREME_3.ordinal()) || type == RoomType.SUPER.ordinal();
	}

	public static boolean isDragon(int type) {
		return type == RoomType.CHI_YAN.ordinal() || type == RoomType.KUANG_BAO.ordinal();
	}

	public static boolean isClassic(int type) {
		return type >= RoomType.NOVICE.ordinal() && type <= RoomType.SENIOR.ordinal() || type == RoomType.ROOM_MYSTERY_LEGEND.ordinal();
	}
	
	// 是否是核弹危机房间
	public static boolean isNuclear(int type) {
		return (type >= RoomType.ROOM_N_BOMB.ordinal() && type <= RoomType.ROOM_N_BOMB_SENIOR.ordinal()) || type == RoomType.ROOM_PERSONAL.ordinal() || type == RoomType.ROOM_N_MYSTERY_LEGEND.ordinal() || type == RoomType.ROOM_ANCIENT_RELICS.ordinal();
	}

	// 是否是秘境传说房间
	public static boolean isMysteryLegend(int type) {
		return false;
		//return type == RoomType.ROOM_MYSTERY_LEGEND.ordinal() || type == RoomType.ROOM_N_MYSTERY_LEGEND.ordinal();
	}

	public List<String> GetRoomList() {
		return m_roomList;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP, ValueType.LONG, false, false, true);// 玩家进入魔晶场时间戳

		IRecord rec = kernel.declareRecord(script, "MojinRoomRecords", 5, 4, false, false, true);
		rec.setColType(MojinRoomRecordColType.COL_ROOMID.ordinal(), ValueType.INT); 			// 魔晶场id
		rec.setColType(MojinRoomRecordColType.COL_ENTER_TIMESTAMP.ordinal(), ValueType.LONG); // 进入时间
		rec.setColType(MojinRoomRecordColType.COL_TOTAL_PLAY.ordinal(), ValueType.LONG); 		// 第一次进入时的总玩
		rec.setColType(MojinRoomRecordColType.COL_TOTAL_COST.ordinal(), ValueType.LONG); 		// 累计游玩量
		rec.setColType(MojinRoomRecordColType.COL_ISATCIVE.ordinal(), ValueType.BOOL); 		// 是否是活跃用户

		rec = kernel.declareRecord(script, NUCLEAR_ROOM_PLAY_RECORD, NuclearRoomPlayRecordCol.COL_END.ordinal(), 100, false, true, true);
		rec.setColType(NuclearRoomPlayRecordCol.COL_ROOM_ID.ordinal(), ValueType.INT);// 魔晶场id
		rec.setColType(NuclearRoomPlayRecordCol.COL_ROOM_DF.ordinal(), ValueType.INT);// 魔晶场难度
		rec.setColType(NuclearRoomPlayRecordCol.COL_TODAY_PLAY_TIME.ordinal(), ValueType.INT);// 当天游玩时间(单位：秒)
		rec.setColType(NuclearRoomPlayRecordCol.COL_TOTAL_PLAY.ordinal(), ValueType.LONG);// 玩家当日总消耗魔晶
		rec.setColType(NuclearRoomPlayRecordCol.COL_TOTAL_WIN.ordinal(), ValueType.LONG);// 玩家当日总获得魔晶
		rec.setColType(NuclearRoomPlayRecordCol.COL_ENTER_TIMES.ordinal(), ValueType.INT);// 玩家当日进入次数
		rec.setColType(NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal(), ValueType.LONG);// 最近一次记录时间
	}

	void OnReqRoomFishList(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data)
			throws InvalidProtocolBufferException {
		CustomMsg.Int32 msg = CustomMsg.Int32.parseFrom(data);
		if (needUpdate) {
			updateRoomFishList(kernel);
			needUpdate = false;
		}
		int roomId = msg.getValue();
		JsonObject json = new JsonObject();
		List<FishInfo> fishs = m_mapRoomFishList.get(roomId);
		if (fishs != null) {
			json.addProperty("code", 0);
			json.add("data", JsonUtil.encodeToElement(fishs));
		} else {
			json.addProperty("code", 1);
		}
		UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
	}

	void OnReqGoldRoomConfig(IKernel kernel, IGameObject player, int msgId, int reqId, byte[] data) {
		int bv = m_BulletValModule.GetMaxBv(player);
		GoldRoomConfig goldRoomConfig = goldRoomConfigMap.get(RoomType.SENIOR.ordinal());
		if (bv >= goldRoomConfig.minBv) {
			UtilFunc.respRpcStringToClient(kernel, player, reqId, JsonUtil.encodeToStr(goldRoomConfigMap));
		}else{
			Map<Integer, GoldRoomConfig> temp = new HashMap<>();
			for (Integer roomType : goldRoomConfigMap.keySet()){
				GoldRoomConfig gc = goldRoomConfigMap.get(roomType);
				if (RoomModule.isClassic(roomType)){
					GoldRoomConfig _gc = new GoldRoomConfig();
					_gc.roomId = gc.roomId;
					_gc.vipLimit = gc.vipLimit;
					_gc.minBv    = gc.minBv;
					_gc.maxBv    = gc.maxBv;
					_gc.maxGold  = -99L;//未解锁神秘洞窟就不限制金币
					temp.put(roomType,_gc);
				}else{
					temp.put(roomType,gc);
				}
			}
			UtilFunc.respRpcStringToClient(kernel, player, reqId, JsonUtil.encodeToStr(temp));
		}
	}

	public void OnReqRoomPlayers(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
		CustomMsg.ReqRoomPlayers.Builder build = CustomMsg.ReqRoomPlayers.newBuilder();
		do {
			IPubData pubdata = kernel.getPubData( "RoomData");
			if (pubdata == null) {
				break;
			}

			IPubRecord rec = pubdata.getRecord("PlayerCount");
			if (rec == null) {
				break;
			}

			int rows = rec.getRows();
			for (int i = 0; i < rows; ++i) {
				int type = rec.getInt(i, 0);
				int count = rec.getInt(i, 1);
				int virtualCount = rec.getInt(i, 2);// 增加虚拟人数数量

				build.addType(type);
				build.addCount(count + virtualCount);
			}
		} while (false);

		kernel.response(player, reqid, build.build().toByteArray());
	}

	public void OnRoomLoad(IKernel kernel, IGameObject room) {
		String fmt = (String) room.getProperty("Formation");
		int[] array = null;
		if (!fmt.isEmpty()&&!fmt.equals("empty")) {
			String[] fmts = fmt.split(",");
			array = new int[fmts.length];
			for (int i = 0; i < fmts.length; ++i) {
				array[i] = Integer.parseInt(fmts[i]);
			}
		} else {
			array = new int[0];
		}

		room.setProperty("Formation", array);

		String pubdataname = kernel.getSerName() + "_room_" + room.getString("Id");
		IPubData pubData = kernel.getPubData(pubdataname);
		if (pubData == null) {
			ServerMsg.PubAddRoom.Builder build = ServerMsg.PubAddRoom.newBuilder();
			build.setName(pubdataname);
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_ADD_ROOM.ordinal(), build.build().toByteArray());
		} else {
			room.setProperty(PLAYER_PROPERTY_TOTALPLAY, pubData.getLong(PLAYER_PROPERTY_TOTALPLAY));
			room.setProperty(PLAYER_PROPERTY_TOTALWIN, pubData.getLong(PLAYER_PROPERTY_TOTALWIN));

			long todayDate = pubData.getLong("TodayDate");
			long now = UtilFunc.getZeroTime(kernel.getServerTime());
			if (now == todayDate) {
				room.setProperty("TodayPlay", pubData.getLong("TodayPlay"));
				room.setProperty("TodayWin", pubData.getLong("TodayWin"));
			}
		}
		int roomType = room.getInt(DESK_TYPE_KEY);
		if (isNuclear(roomType)){
			pubData = kernel.getPubData("MojinRoomActiveData");
			if (pubData != null) {
				IPubRecord rec = pubData.getRecord("MojinRoomActiveRecord");
				if (rec != null) {
					int rowIndex = rec.findRow(0, 0, room.getString("Id"));
					if (rowIndex != -1) {
						room.setProperty("TodayTotalPlayerCount", rec.getInt(rowIndex, 1));
						room.setProperty("TodayActivePlayer", rec.getInt(rowIndex, 2));
						room.setProperty("TodayTotalPlayTime", rec.getLong(rowIndex, 3));
						room.setProperty("TodayTotalEnterTimes", rec.getInt(rowIndex, 4));
						room.setProperty("MaxPlayerCount", rec.getInt(rowIndex, 5));
					}
				}
			}
		}
	}

	void UpdateTodayPW(IKernel kernel, IGameObject room) {
		long now = UtilFunc.getZeroTime(kernel.getServerTime());

		String pubdataname = kernel.getSerName() + "_room_" + room.getString("Id");
		ServerMsg.PubUpdateDayPW.Builder build = ServerMsg.PubUpdateDayPW.newBuilder();
		build.setName(pubdataname);
		build.setPlay(room.getLong("TodayPlay"));
		build.setWin(room.getLong("TodayWin"));
		build.setDate(now);
		build.setSerName(kernel.getSerName());
		build.setRoomType(room.getInt(DESK_TYPE_KEY));

		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_UPDATE_TOTALPW.ordinal(), build.build().toByteArray());
	}
	
	boolean StoreMojinRoomRecord(IKernel kernel, int order, String data){
		if (order != UtilFunc.StopListenerOrder.ROOM_MODULE.ordinal()) {
			return false;
		}
		List<Map<String, Object>> datas = new ArrayList<>();
		for (String roomid : m_roomList) {
			IGameObject room = kernel.getPreloadObject(roomid);
			if (room == null) {
				continue;
			}
			if (isNuclear(room.getInt(DESK_TYPE_KEY))){
				Map<String, Object> tmp = new HashMap<>();
				int todayTotalPlayerCount = room.getInt("TodayTotalPlayerCount");
				int todayActivePlayer = room.getInt("TodayActivePlayer");
				int maxPlayerCount = room.getInt("MaxPlayerCount");
				long todayTotalPlayTime = room.getLong("TodayTotalPlayTime");
				int todayTotalEnterTimes = room.getInt("TodayTotalEnterTimes");
				tmp.put("TodayTotalPlayerCount", todayTotalPlayerCount);
				tmp.put("TodayActivePlayer", todayActivePlayer);
				tmp.put("MaxPlayerCount", maxPlayerCount);
				tmp.put("TodayTotalPlayTime", todayTotalPlayTime);
				tmp.put("TodayTotalEnterTimes", todayTotalEnterTimes);
				tmp.put("roomId", room.getString("Id"));
				datas.add(tmp);
			}
			UpdateTodayPW(kernel, room);
		}
		if (datas.size() > 0){
			CustomMsg.String.Builder build = CustomMsg.String.newBuilder();
			build.setValue(JsonUtil.encodeToStr(datas));
			byte[] msg = build.build().toByteArray();
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_MOJIN_ROOM_ACTIVE_RECORD.ordinal(),msg);
		}
		return true;
	}

	void OnChangeDay(IKernel kernel, int day) {
		MojinRoomRecord record = null;
		int allTodayTotalPlayerCount = 0;
		int allTodayActivePlayer = 0;
		int allMaxPlayerCount = 0;
		long allTodayPlay = 0l;
		long allTodayWin = 0l;
		float allAvgPlayTime = 0f;
		float allAvgEnterTimes = 0f;
		for (String roomid : m_roomList) {
			IGameObject room = kernel.getPreloadObject(roomid);
			if (room == null) {
				continue;
			}
			
			// 每日记录一下魔晶场的游玩情况
			int id = Integer.parseInt(roomid.split("room")[1]);
			if (isNuclear(id)){
				int todayTotalPlayerCount = room.getInt("TodayTotalPlayerCount");
				int todayActivePlayer = room.getInt("TodayActivePlayer");
				int maxPlayerCount = room.getInt("MaxPlayerCount");
				long todayTotalPlayTime = room.getLong("TodayTotalPlayTime");
				int todayTotalEnterTimes = room.getInt("TodayTotalEnterTimes");
				long todayPlay = room.getLong("TodayPlay");
				long todayWin = room.getLong("TodayWin");
				float avgPlayTime = 0f;
				float avgEnterTimes = 0f;
				if (todayTotalPlayerCount != 0) {
					avgPlayTime = todayTotalPlayTime / todayTotalPlayerCount / 1000; // 单位s
					avgEnterTimes = todayTotalEnterTimes / todayTotalPlayerCount;
				}
				logger.info("DealRecordRoomActiveData {} {} {} {} {} {} {}", 
						todayTotalPlayerCount, todayActivePlayer, maxPlayerCount, todayTotalPlayTime, todayTotalEnterTimes, todayPlay, todayWin);
				record = new MojinRoomRecord();
				record.setDate(TimeUtils.yesterDay(kernel.getServer().getDayFormat()));
				record.setRoomType(id);
				record.setRoomDf(room.getInt("RoomDf"));
				record.setTotalPlayerCount(todayTotalPlayerCount);
				record.setTodayActive(todayActivePlayer);
				record.setMaxPlayerCount(maxPlayerCount);
				record.setAvgPlayTime(avgPlayTime+"");
				record.setAvgEnterTimes(avgEnterTimes+"");
				record.setTodayPlay(todayPlay);
				record.setTodayWin(todayWin);
				DealRecordRoomActiveData(kernel, record);
				
				allTodayTotalPlayerCount += todayTotalPlayerCount;
				allTodayActivePlayer += todayActivePlayer;
				allMaxPlayerCount += maxPlayerCount;
				allTodayPlay += todayPlay;
				allTodayWin += todayWin;
				allAvgPlayTime += avgPlayTime;
				allAvgEnterTimes += avgEnterTimes;
			}
			
			UpdateTodayPW(kernel, room);
			room.setProperty("TodayPlay", 0L);
			room.setProperty("TodayWin", 0L);
			room.setProperty("TodayTotalPlayerCount", 0);
			room.setProperty("TodayActivePlayer", 0);
			room.setProperty("TodayTotalPlayTime", 0L);
			room.setProperty("TodayTotalEnterTimes", 0);
			room.setProperty("CurPlayerCount", 0);
			room.setProperty("MaxPlayerCount", 0);
		}
		
		// 记录所有房间
		record = new MojinRoomRecord();
		record.setDate(TimeUtils.yesterDay(kernel.getServer().getDayFormat()));
		record.setRoomType(-1);
		record.setRoomDf(-1);
		record.setTotalPlayerCount(allTodayTotalPlayerCount);
		record.setTodayActive(allTodayActivePlayer);
		record.setMaxPlayerCount(allMaxPlayerCount);
		record.setAvgPlayTime(allAvgPlayTime+"");
		record.setAvgEnterTimes(allAvgEnterTimes+"");
		record.setTodayPlay(allTodayPlay);
		record.setTodayWin(allTodayWin);
		DealRecordRoomActiveData(kernel, record);
	}
	
	void DealRecordRoomActiveData(IKernel kernel, MojinRoomRecord record){
		kernel.addMoJinRoomActiveData(record);
	}

	void OnCmdChangeDay(IKernel kernel, IGameObject player) {
		PlayerDailyDataLogic(kernel, player);
	}

	void PlayerDailyDataLogic(IKernel kernel, IGameObject player) {
		long curTime = kernel.getServerTime();
		String date = kernel.getServer().getDayFormat().format(curTime - 1000);
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		String nickname = player.getString(PLAYER_PROPERTY_NAME);
		int vip_level = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		String itemHas = UtilFunc.getHN(player);
		long mojin = player.getLong(PLAYER_PROPERTY_BOMB_COIN);

		List<PlayerDailyPlayData> storeDataList = new ArrayList<>();
		IRecord totalPw = player.getRecord("TotalPlayWin");

		IRecord record = player.getRecord(NUCLEAR_ROOM_PLAY_RECORD);
		int rows = record.getRows();
		for (int i = 0; i < rows; i++) {
			int roomId = record.getInt(i, NuclearRoomPlayRecordCol.COL_ROOM_ID.ordinal());
			long playerPlayNew = totalPw.getLong(roomId, 0);
			long playerWinNew = totalPw.getLong(roomId, 1);
			int roomDf = record.getInt(i, NuclearRoomPlayRecordCol.COL_ROOM_DF.ordinal());
			long todayPlayTime = record.getInt(i, NuclearRoomPlayRecordCol.COL_TODAY_PLAY_TIME.ordinal());
			//这个记录的是首次进入房间时的总玩总赢
			long todayPlay = record.getLong(i, NuclearRoomPlayRecordCol.COL_TOTAL_PLAY.ordinal());
			long todayWin = record.getLong(i, NuclearRoomPlayRecordCol.COL_TOTAL_WIN.ordinal());
			int enterTimes = record.getInt(i, NuclearRoomPlayRecordCol.COL_ENTER_TIMES.ordinal());

			long dayCost = playerPlayNew - todayPlay;
			long dayWin = playerWinNew - todayWin;
			if (dayCost != 0L || dayWin != 0L) {
				PlayerDailyPlayData pdd = new PlayerDailyPlayData();
				pdd.setDate(date);
				pdd.setUid(uid);
				pdd.setNickName(nickname);
				pdd.setVipLevel(vip_level);
				pdd.setRoomId(roomId);
				pdd.setRoomDf(roomDf);
				pdd.setItemHas(itemHas);
				pdd.setMojin(mojin);
				pdd.setDayCost(dayCost);
				pdd.setDayWin(dayWin);
				pdd.setDayPlayTime(todayPlayTime);
				pdd.setDayEnterTimes(enterTimes);
				pdd.setProxyId(player.getInt(PLAYER_PROPERTY_PROPERTY_PROXY_ID));
				storeDataList.add(pdd);
			}
			record.setValue(i, NuclearRoomPlayRecordCol.COL_TODAY_PLAY_TIME.ordinal(), 0);
			record.setValue(i, NuclearRoomPlayRecordCol.COL_TOTAL_PLAY.ordinal(), playerPlayNew);
			record.setValue(i, NuclearRoomPlayRecordCol.COL_TOTAL_WIN.ordinal(), playerWinNew);
			record.setValue(i, NuclearRoomPlayRecordCol.COL_ENTER_TIMES.ordinal(), 0);
			record.setValue(i, NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal(), curTime);
		}
		player.setProperty(PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP, curTime);
		if (storeDataList.size() > 0) {
			//logger.info("RoomModule OnCmdChangeDay playerDailyPlayData {}", JsonUtil.encodeToStr(storeDataList));
			kernel.addOrUpdatePlayerDailyPlayData(storeDataList);
		}
	}

	public void OnPlayerSitDown(IKernel kernel, IGameObject player, IGameObject desk) {
		IGameObject room = desk.getParent();
		IRecord deskRec = room.getRecord("DeskRec");
		int pos = deskRec.findRow(0, DeskColType.COL_DESKID.ordinal(), desk.getObjectID());
		if (pos != -1) {
			int playerCount = desk.getSeatCount() - desk.getFreeSeatCount();
			deskRec.setValue(pos, DeskColType.COL_DESK_PLAYER_COUNT.ordinal(), playerCount);
		}
		int uid = player.getInt(PLAYER_PROPERTY_UID);
		ServerMsg.SitDown.Builder build = ServerMsg.SitDown.newBuilder();
		build.setDeskid(desk.getObjectID());
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		build.setPlayerid(player.getObjectID());
		build.setUid(uid);
		build.setVip(player.getInt(PLAYER_PROPERTY_VIPLEVEL));
		build.setName(player.getString(PLAYER_PROPERTY_NAME));
		build.setLevel(player.getInt(PLAYER_PROPERTY_LEVEL));
		build.setHead(player.getInt(PLAYER_PROPERTY_HEADID));

		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.MMSG_SIT_DOWN.ordinal(), build.build().toByteArray());

		// 通知公共区+1
		int roomid = room.getInt(DESK_TYPE_KEY);
		ServerMsg.UpdateRoom.Builder update = ServerMsg.UpdateRoom.newBuilder();
		update.setRoomid(roomid);
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_ENTER_ROOM.ordinal(), update.build().toByteArray());
		
		// 魔晶场统计活跃等
		if (isNuclear(roomid)){
			room.setProperty("RoomDf", desk.getInt("Level"));
			// 更新房间当前人数和峰值人数
			int maxPlayerCount = room.getInt("MaxPlayerCount");
			int curPlayerCount = room.getInt("CurPlayerCount") + 1;
			room.setProperty("CurPlayerCount", curPlayerCount);
			if (curPlayerCount > maxPlayerCount){
				room.setProperty("MaxPlayerCount", curPlayerCount);
			}
			int todayTotalEnterTimes = room.getInt("TodayTotalEnterTimes");
			int todayTotalPlayerCount = room.getInt("TodayTotalPlayerCount");
			long curTime = kernel.getServerTime();
			long playerTotalPlay = player.getLong(PLAYER_PROPERTY_BOMBTOTALPLAY);
			IRecord rc = player.getRecord("MojinRoomRecords");
			int rowIndex = rc.findRow(0, 0, roomid);
			if (rowIndex != -1){
				long lastTime = rc.getLong(rowIndex, MojinRoomRecordColType.COL_ENTER_TIMESTAMP.ordinal());
				// 不是同一天 则清除数据
				if (!TimeUtils.isSameDay(lastTime)){
					rc.setValue(rowIndex, MojinRoomRecordColType.COL_TOTAL_COST.ordinal(), 0l);
					rc.setValue(rowIndex, MojinRoomRecordColType.COL_ISATCIVE.ordinal(), false);
					room.setProperty("TodayTotalPlayerCount", todayTotalPlayerCount + 1);
				}
				rc.setValue(rowIndex, MojinRoomRecordColType.COL_ENTER_TIMESTAMP.ordinal(), curTime);
				rc.setValue(rowIndex, MojinRoomRecordColType.COL_TOTAL_PLAY.ordinal(), playerTotalPlay);
			} else {
				room.setProperty("TodayTotalPlayerCount", todayTotalPlayerCount + 1);
				rc.addRow(roomid, curTime, playerTotalPlay, 0l, false);
			}
			room.setProperty("TodayTotalEnterTimes", todayTotalEnterTimes + 1);

			// 玩家每日游玩数据
			player.setProperty(PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP, curTime);
			IRecord totalPw = player.getRecord("TotalPlayWin");
			long playerPlay = totalPw.getLong(roomid, 0);
			long playerWin = totalPw.getLong(roomid, 1);

			// 初始化一条房间游玩记录
			IRecord nuclearRoomPlayRecord = player.getRecord(NUCLEAR_ROOM_PLAY_RECORD);
			int row = nuclearRoomPlayRecord.findRow(0, NuclearRoomPlayRecordCol.COL_ROOM_ID.ordinal(), roomid);
			if (row == -1) {
				nuclearRoomPlayRecord.addRow( roomid, desk.getInt("Level"), 0, playerPlay, playerWin, 1, curTime);
			} else {
				long recordTime = nuclearRoomPlayRecord.getLong(row, NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal());
				if (!TimeUtils.isSameDay(curTime, recordTime)) {
					nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal(), curTime);
					nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_TOTAL_PLAY.ordinal(), playerPlay);
					nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_TOTAL_WIN.ordinal(), playerWin);
					player.setProperty(PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP, curTime);
				}
			}
		}
	}

	public void OnPlayerStandUp(IKernel kernel, IGameObject player, IGameObject desk) {
		IGameObject room = desk.getParent();
		IRecord deskRec = room.getRecord("DeskRec");
		int pos = deskRec.findRow(0, DeskColType.COL_DESKID.ordinal(), desk.getObjectID());
		if (pos != -1) {
			int playerCount = desk.getSeatCount() - desk.getFreeSeatCount();
			deskRec.setValue(pos, DeskColType.COL_DESK_PLAYER_COUNT.ordinal(), playerCount);
		}
		ServerMsg.StandUp.Builder build = ServerMsg.StandUp.newBuilder();
		build.setDeskid(desk.getObjectID());
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		int roomType = desk.getInt(DESK_TYPE_KEY);
		if (RoomModule.isSupreme(roomType) || RoomModule.isNuclear(roomType)) {
			// 至尊选座，离开时同步总玩总赢
			build.setTotalplay(desk.getLong(PLAYER_PROPERTY_TOTALPLAY));
			build.setTotalwin(desk.getLong(PLAYER_PROPERTY_TOTALWIN));
		}
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.MMSG_STAND_UP.ordinal(), build.build().toByteArray());
		// 通知公共区-1
		ServerMsg.UpdateRoom.Builder update = ServerMsg.UpdateRoom.newBuilder();
		update.setRoomid(roomType);
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_LEAVE_ROOM.ordinal(), update.build().toByteArray());
		// 魔晶场统计活跃等
		if (RoomModule.isNuclear(roomType)){
			IRecord rc = player.getRecord("MojinRoomRecords");
			// 更新房间当时在线人数
			int curPlayerCount = room.getInt("CurPlayerCount") - 1;
			curPlayerCount = curPlayerCount < 0 ? 0 : curPlayerCount;
			room.setProperty("CurPlayerCount", curPlayerCount);
			long curTime = kernel.getServerTime();
			int rowIndex = rc.findRow(0, 0, roomType);
			if (rowIndex != -1){
				long enterTime = rc.getLong(rowIndex, MojinRoomRecordColType.COL_ENTER_TIMESTAMP.ordinal());
				long gameTime = curTime - enterTime;
				// 如果玩家跨天了 则修改进入时间为今天 不然会多记
				if (!TimeUtils.isSameDay(enterTime)){
					rc.setValue(rowIndex, MojinRoomRecordColType.COL_ENTER_TIMESTAMP.ordinal(), curTime);
				}
				room.setProperty("TodayTotalPlayTime",  room.getLong("TodayTotalPlayTime") + gameTime);
				boolean isActive = rc.getBool(rowIndex, MojinRoomRecordColType.COL_ISATCIVE.ordinal());
				long totalCost = rc.getLong(rowIndex, MojinRoomRecordColType.COL_TOTAL_COST.ordinal());
				long curTotalPlay = player.getLong(PLAYER_PROPERTY_BOMBTOTALPLAY);
				long olderTotalPlay = rc.getLong(rowIndex, MojinRoomRecordColType.COL_TOTAL_PLAY.ordinal());
				long dval = curTotalPlay - olderTotalPlay;
				if (!isActive && totalCost + dval > 1000000) {
					room.setProperty("TodayActivePlayer",  room.getInt("TodayActivePlayer") + 1);
					isActive = true;
					rc.setValue(rowIndex, MojinRoomRecordColType.COL_ISATCIVE.ordinal(), isActive);
				}
				if (!isActive) {
					rc.setValue(rowIndex, MojinRoomRecordColType.COL_TOTAL_COST.ordinal(), totalCost + dval);
				}
			}

			int playTime = (int)((curTime - player.getLong(PLAYER_PROPERTY_ENTER_NUCLEAR_ROOM_TIMESTAMP)) / 1000);

			IRecord totalPw = player.getRecord("TotalPlayWin");
			long playerPlayNew = totalPw.getLong(roomType, 0);
			long playerWinNew = totalPw.getLong(roomType, 1);

			int roomDf = desk.getInt("Level");

			IRecord nuclearRoomPlayRecord = player.getRecord(NUCLEAR_ROOM_PLAY_RECORD);
			int row = nuclearRoomPlayRecord.findRow(0, NuclearRoomPlayRecordCol.COL_ROOM_ID.ordinal(), roomType);
			if (row != -1) {
				long recordTime = nuclearRoomPlayRecord.getLong(row, NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal());
				if (!TimeUtils.isSameDay(curTime, recordTime)) {
					return;
				}
				long recPlayTime = nuclearRoomPlayRecord.getInt(row, NuclearRoomPlayRecordCol.COL_TODAY_PLAY_TIME.ordinal());
				long recPlay = nuclearRoomPlayRecord.getLong(row, NuclearRoomPlayRecordCol.COL_TOTAL_PLAY.ordinal());
				long recWin = nuclearRoomPlayRecord.getLong(row, NuclearRoomPlayRecordCol.COL_TOTAL_WIN.ordinal());
				int enterTimes = nuclearRoomPlayRecord.getInt(row, NuclearRoomPlayRecordCol.COL_ENTER_TIMES.ordinal());
				nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_ROOM_DF.ordinal(), roomDf);
				nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_TODAY_PLAY_TIME.ordinal(), (int)recPlayTime + playTime);
				nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_ENTER_TIMES.ordinal(), enterTimes + 1);
				nuclearRoomPlayRecord.setValue(row, NuclearRoomPlayRecordCol.COL_RECORD_DATE.ordinal(), curTime);
				List<PlayerDailyPlayData> storeDataList = new ArrayList<>();
				PlayerDailyPlayData pdd = new PlayerDailyPlayData();
				pdd.setDate(kernel.getServer().getDayFormat().format(curTime));
				pdd.setUid(player.getInt(PLAYER_PROPERTY_UID));
				pdd.setNickName(player.getString(PLAYER_PROPERTY_NAME));
				pdd.setVipLevel(player.getInt(PLAYER_PROPERTY_VIPLEVEL));
				pdd.setRoomId(roomType);
				pdd.setRoomDf(roomDf);
				pdd.setItemHas(UtilFunc.getHN(player));
				pdd.setMojin(player.getLong(PLAYER_PROPERTY_BOMB_COIN));
				pdd.setDayCost(playerPlayNew - recPlay);
				pdd.setDayWin(playerWinNew - recWin);
				pdd.setDayPlayTime(recPlayTime + playTime);
				pdd.setDayEnterTimes(enterTimes);
				pdd.setProxyId(player.getInt(PLAYER_PROPERTY_PROPERTY_PROXY_ID));
				storeDataList.add(pdd);
				kernel.addOrUpdatePlayerDailyPlayData(storeDataList);
			}
		}
	}
	
	public void OnDeskDestroy(IKernel kernel, IGameObject desk) {
		IGameObject room = desk.getParent();
		if (room == null){
			return;
		}
		IRecord deskRec  = room.getRecord("DeskRec");
		int pos = deskRec.findRow(0, DeskColType.COL_DESKID.ordinal(), desk.getObjectID());
		if (pos != -1) {
			deskRec.removeRow(pos);
		}
	}

	public void OnRecvCreateDesk(IKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {


	}
	public void OnCreateMysteryLegendDesk(IKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.AddMysteryLegendDesk deskData = ServerMsg.AddMysteryLegendDesk.parseFrom(data);
		int roomType = deskData.getRoomType();

		LogManager.fishLogger.info("OnCreateMysteryLegendDesk|{}", roomType);
		IGameObject room = kernel.getPreloadObject("room" + roomType);
		if (room == null) {
			logger.error("room not found");
			return;
		}
		IGameObject desk = kernel.createObjectByConfig(room.getString(PLAYER_PROPERTY_DESKID), room,
				"ObjId", deskData.getPlayerId(), "MaxBV", deskData.getMaxBV(), "MinBV", deskData.getMinBv());
		if (desk != null) {
			IRecord deskRec = room.getRecord("DeskRec");
			deskRec.addRow(desk.getObjectID(), desk.getSeatCount(), 0);
		}
	}

	public void OnRecvAllRoomRunInfo(IKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		logger.info("OnRecvRoomRunInfo");
		ServerMsg.RoomDatas response = ServerMsg.RoomDatas.parseFrom(data);
		List<ServerMsg.RoomData> roomDataProList = response.getRoomDatasList();
		ServerMsg.RoomDatas.Builder build = ServerMsg.RoomDatas.newBuilder();
		for (ServerMsg.RoomData info : roomDataProList) {
			ServerMsg.RoomData.Builder roomDataBuilder = ServerMsg.RoomData.newBuilder();
			roomDataBuilder.setId(info.getId());
			IGameObject room = kernel.getPreloadObject("room" + info.getId());
			if (room != null) {
				int vipLevel = room.getInt("VipLimit");
				int autoKick = room.getInt("AutoKick");
				int gunValueMin = room.getInt("MinBV");
				int gunValueMax = room.getInt("MaxBV");
				roomDataBuilder.setAutoKick(autoKick);
				roomDataBuilder.setMaxGun(gunValueMax);
				roomDataBuilder.setMinGun(gunValueMin);
				roomDataBuilder.setVipLevel(vipLevel);
				roomDataBuilder.setOnline(info.getOnline());
				roomDataBuilder.setTotalPlay(info.getTotalPlay());
				roomDataBuilder.setTotalWin(info.getTotalWin());
				roomDataBuilder.setTotalGet(info.getTotalGet());
				build.addRoomDatas(roomDataBuilder.build());
				logger.info("game: totalPlay: {} tatalWin: {}", roomDataBuilder.getTotalPlay(), roomDataBuilder.getTotalWin());
			}
		}
		// 返回消息到Back，加上在线人数和总玩总赢
		logger.info("send G2B_ALLROOM_DATA to Back server ...");
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_BACK, ServerMsgDef.G2B_ALLROOM_DATA.ordinal(), build.build().toByteArray());
	}
}
