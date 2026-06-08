package game.modules;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.*;
import game.custommsg.S2CMsgDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

public class DeskModule implements ILogicModule {
	enum SlaveColType {
		COL_INDEX, COL_TOTAL_PLAY, COL_TOTAL_WIN, COL_MAX
	}

	static Logger logger = LoggerFactory.getLogger(DeskModule.class);

	public static String DESK_SKILL_FROZEN_KEY = "desk_skill_frozen_key";

	public DeskModule(IKernel kernel) {
		kernel.addClass("FishDesk", "Desk");
		kernel.addClass("ArenaDesk", "FishDesk");
		kernel.addClass("BossDesk", "FishDesk");
	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "FishDesk", this, "OnDeskClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_CLASS_READY, "FishDesk", this, "OnDeskClassReady");
		kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "FishDesk", this, "OnDeskLoad");
		kernel.regEvent(KernelEvent.KEVENT_ON_DESTROY, "FishDesk", this, "OnDeskDestroy");
		kernel.regEvent(KernelEvent.KEVENT_ON_ENTER, "FishDesk", this, "OnDeskEnter");

		kernel.preLoadConfig("res/Game/Desk.xml");
		return true;
	}

	@Override
	public void onDestroy() {

	}

	public void OnDeskClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Type", ValueType.INT, true, true, false);
		kernel.declareProperty(script, "Rule", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "Coin", ValueType.STRING, true, true, false);
		kernel.declareProperty(script, "State", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "NextState", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "StateStartTime", ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "BulletMax", ValueType.INT, false, false, false);
		// 上一次鱼阵
		kernel.declareProperty(script, "LastFormation", ValueType.INT, false, false, false);
		kernel.declareProperty(script, DESK_FISH_INDEX, ValueType.INT, false, false, false);
		kernel.declareProperty(script, "FrozenEnd", ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "Cleared", ValueType.BOOL, false, false, false);

		kernel.declareProperty(script, "BossTime", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "BossTip", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "BossID", ValueType.STRING, false, false, false);

		kernel.declareProperty(script, "SpiritBossTime", ValueType.LONG, true, true, false);
		kernel.declareProperty(script, "ObjId", ValueType.LONG, false, false, false);


		// 至尊选座桌子id
		kernel.declareProperty(script, PLAYER_PROPERTY_DESKID, ValueType.INT, true, true, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALPLAY, ValueType.LONG, false, false, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_TOTALWIN, ValueType.LONG, false, false, false);
		kernel.declareProperty(script, "MinBV", ValueType.INT, true, true, false);
		kernel.declareProperty(script, "MaxBV", ValueType.INT, true, true, false);
		kernel.declareProperty(script, PLAYER_PROPERTY_LEVEL, ValueType.INT, false, false, false);
		kernel.declareProperty(script, "Limit", ValueType.STRING, false, false, false);
		kernel.declareProperty(script, "GameType", ValueType.INT, true, true, false);
		kernel.declareProperty(script, "AutoKick", ValueType.INT, false, false, false);
		kernel.declareProperty(script, "DeskBG", ValueType.INT, true, true, false);
		kernel.declareProperty(script, "KickOutTime", ValueType.LONG, true, true, false);

		// 火龙桌子 add by 赵俊@20190527
		kernel.declareProperty(script, "DeskType", ValueType.INT, true, true, false); // 桌子类型
		//世界boss离场世界
		kernel.declareProperty(script, DESK_BOSS_LEAVE_TIME, ValueType.LONG, true, true, false);
		kernel.declareProperty(script, DESK_NOT_OUT_FISH_END_TIME, ValueType.LONG, true, true, false);

		IRecord slaveRec = kernel.declareRecord(script, "SlaveRec", SlaveColType.COL_MAX.ordinal(), 10, false, false,false);
		slaveRec.setColType(SlaveColType.COL_INDEX.ordinal(), ValueType.SHORT);
		slaveRec.setColType(SlaveColType.COL_TOTAL_PLAY.ordinal(), ValueType.INT);
		slaveRec.setColType(SlaveColType.COL_TOTAL_WIN.ordinal(), ValueType.INT);
		//DESK_SKILL_FROZEN_KEY
		kernel.declareHeartBeat(DESK_SKILL_FROZEN_KEY,this, "OnCheckFrozen");
	}

	void OnCheckFrozen(IKernel kernel, IGameObject desk) throws ParseException {
		long lastEnd = desk.getLong("FrozenEnd");
		long now = kernel.getServerTime();
		if (lastEnd > 0L && lastEnd < now){
			kernel.broadCastByDesk(desk,S2CMsgDef.S2C_SKILL_FROEN_OVER_MSG.ordinal(),null);
			desk.setProperty("FrozenEnd",0L);
		}
	}

	public void OnDeskClassReady(IKernel kernel, String script) {
		kernel.setVisible(script, PLAYER_PROPERTY_NAME, true, true, false);
	}

	public void OnDeskLoad(IKernel kernel, IGameObject desk) {
		desk.setProperty("LastFormation", -1);
		// Load total play/win from pubdata
	}

	public void OnDeskEnter(IKernel kernel, IGameObject desk, IGameObject room) {
		//logger.info("OnDeskEnter {}",desk.GetObjectID());
		int roomType = room.getInt(DESK_TYPE_KEY);
		if (RoomModule.isDragon(roomType)) {
			// 火龙房间由创建逻辑处同步
			return;
		}
		ServerMsg.AddDesk.Builder build = ServerMsg.AddDesk.newBuilder();
		build.setRoomType(room.getInt(DESK_TYPE_KEY));
		build.setDeskid(desk.getObjectID());
		build.setSeatcount(desk.getSeatCount());
		build.setPlayercount(0);
		int deskType = desk.getInt(DESK_TYPE_KEY);
		if (RoomModule.isSupreme(deskType) || RoomModule.isNuclear(deskType)) {
			// 至尊选座
			build.setDesk(desk.getInt(PLAYER_PROPERTY_DESKID));
		}
		if (RoomModule.isMysteryLegend(deskType)) {
			build.setObjId(desk.getLong("ObjId"));
			build.setDeskType(desk.getInt(DESK_TYPE_KEY));
			build.setMaxBv(desk.getInt("MaxBV"));
			build.setMinBv(desk.getInt("MinBV"));
		}

		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.MMSG_ADD_DESK.ordinal(), build.build().toByteArray());
	}

	public void OnDeskDestroy(IKernel kernel, IGameObject desk) {
		ServerMsg.DelDesk.Builder build = ServerMsg.DelDesk.newBuilder();
		build.setDeskid(desk.getObjectID());
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.MMSG_DEL_DESK.ordinal(), build.build().toByteArray());
	}
}
