package pub.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ValueType;
import framework.pub.*;
import game.modules.utils.UtilFunc;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class BossBattle implements IPubModule {
	
	enum REC_BOSS_COL {
		COL_ID, // 房间ID
		COL_TYPE, // BOSS类型
		COL_OBJID, // 桌子对象号
		COL_OWNER, // 房主uid
		COL_SEAT_COUNT, // 座位数
		COL_IN_COUNT, // 房间中人数
		COL_PASSWD, // 房间密码
		COL_END_TIME, // 结束时间
		COL_END
	}

	Map<Integer, List<Integer>> m_mapUnusedID = new HashMap<>();

	@Override
	public boolean onInit(IPubKernel kernel) {
		kernel.regOnLoadEvent("pubdata", this, "OnPubdataLoad");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_ADD_BOSS_DESK.ordinal(), this,"OAddDesk");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ENTER_BOSS_DESK.ordinal(), this,"OEnterDesk");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_LEAVE_BOSS_DESK.ordinal(), this,"OLeaveDesk");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_DEL_BOSS_DESK.ordinal(), this,"ODelDesk");
		kernel.regStopListener(this, UtilFunc.StopListenerOrder.BOSS_BATTLE.ordinal(),"OnCloseAllBoss");
		return true;
	}

	void OnPubdataLoad(IPubKernel kernel, String none) {
		IPubData pubData = kernel.getPubData("BossData",true);
		pubData.clearRecord();//清理上次的数据
		kernel.storePubData(pubData);
	}

	@Override
	public void onDestroy() {

	}

	void InitDeskRec(IPubRecord rec) {
		rec.setColType(REC_BOSS_COL.COL_ID.ordinal(), ValueType.INT);
		rec.setColType(REC_BOSS_COL.COL_TYPE.ordinal(), ValueType.INT);
		rec.setColType(REC_BOSS_COL.COL_OBJID.ordinal(), ValueType.LONG);
		rec.setColType(REC_BOSS_COL.COL_OWNER.ordinal(), ValueType.INT);
		rec.setColType(REC_BOSS_COL.COL_SEAT_COUNT.ordinal(), ValueType.INT);
		rec.setColType(REC_BOSS_COL.COL_IN_COUNT.ordinal(), ValueType.INT);
		rec.setColType(REC_BOSS_COL.COL_PASSWD.ordinal(), ValueType.STRING);
		rec.setColType(REC_BOSS_COL.COL_END_TIME.ordinal(), ValueType.LONG);
	}

	void OAddDesk(IPubKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
		build.setIntMember(-1);
		ServerMsg.AddBossDesk msg = ServerMsg.AddBossDesk.parseFrom(data);
		int type = msg.getType();
		int uid = msg.getUid();
		long deskid = msg.getDeskid();
		String passwd = msg.getPassw();
		long endline = msg.getEndline();
		int maxCount = msg.getMaxCount();
		IPubData pubData = kernel.getPubData("BossData",false);
		if (null == pubData) {
			pubData = new PubData("BossData");
		}
		IPubRecord rec = pubData.getRecord("RecDesk_" + type);
		if (rec == null) {
			rec = pubData.addRecord("RecDesk_" + type, REC_BOSS_COL.COL_END.ordinal(), 100, true);
			InitDeskRec(rec);
			List<Integer> listUnused = new LinkedList<>();
			for (int i = 0; i < maxCount; ++i) {
				listUnused.add(i + 1);
			}
			m_mapUnusedID.put(type, listUnused);
		}
		if (rec.getRows() >= maxCount) {
			kernel.responseServer(reqid, build.build().toByteArray());
			kernel.storePubData(pubData);
			return;
		}
		int id = m_mapUnusedID.get(type).get(0);
		m_mapUnusedID.get(type).remove(0);
		rec.addRow(id, type, deskid, uid, 4, 0, passwd, endline);
		build.setIntMember(id);
		kernel.storePubData(pubData);
		kernel.responseServer(reqid, build.build().toByteArray());
	}

	void OEnterDesk(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.EnterBossDesk msg = ServerMsg.EnterBossDesk.parseFrom(data);
		int type = msg.getType();
		long id = msg.getDeskid();
		IPubData pubData = kernel.getPubData("BossData",false);
		if (pubData == null) {
			return;
		}
		IPubRecord rec = pubData.getRecord("RecDesk_" + type);
		if (rec == null) {
			return;
		}
		int row = rec.findRow(0, REC_BOSS_COL.COL_OBJID.ordinal(), id);
		if (row == -1) {
			return;
		}
		int seatCount = rec.getInt(row, REC_BOSS_COL.COL_SEAT_COUNT.ordinal());
		int inCount = rec.getInt(row, REC_BOSS_COL.COL_IN_COUNT.ordinal()) + 1;
		if (inCount > seatCount) {
			inCount = seatCount;
		}
		rec.setValue(row, REC_BOSS_COL.COL_IN_COUNT.ordinal(), inCount);
		kernel.storePubData(pubData);
	}

	void OLeaveDesk(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.LeaveBossDesk msg = ServerMsg.LeaveBossDesk.parseFrom(data);
		int type = msg.getType();
		long id = msg.getDeskid();
		IPubData pubData = kernel.getPubData("BossData",false);
		if (pubData == null) {
			return;
		}
		IPubRecord rec = pubData.getRecord("RecDesk_" + type);
		if (rec == null) {
			return;
		}
		int row = rec.findRow(0, REC_BOSS_COL.COL_OBJID.ordinal(), id);
		if (row == -1) {
			return;
		}
		int inCount = rec.getInt(row, REC_BOSS_COL.COL_IN_COUNT.ordinal()) - 1;
		if (inCount < 0) {
			inCount = 0;
		}
		rec.setValue(row, REC_BOSS_COL.COL_IN_COUNT.ordinal(), inCount);
		kernel.storePubData(pubData);
	}

	void ODelDesk(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.DelBossDesk msg = ServerMsg.DelBossDesk.parseFrom(data);
		int type = msg.getType();
		long id = msg.getDeskid();
		IPubData pubData = kernel.getPubData("BossData",false);
		if (pubData == null) {
			return;
		}
		IPubRecord rec = pubData.getRecord("RecDesk_" + type);
		if (rec == null) {
			return;
		}
		int row = rec.findRow(0, REC_BOSS_COL.COL_OBJID.ordinal(), id);
		if (row == -1) {
			return;
		}
		int deskid = rec.getInt(row, REC_BOSS_COL.COL_ID.ordinal());
		rec.removeRow(row);
		m_mapUnusedID.get(type).add(deskid);
		kernel.storePubData(pubData);
	}
	
	boolean OnCloseAllBoss(IPubKernel kernel, int order, String data) {
		if (order != UtilFunc.StopListenerOrder.BOSS_BATTLE.ordinal()) {
			return false;
		}
		IPubData pubData = kernel.getPubData("BossData",false);
		if (pubData == null){
			return true;
		}
		for (int i = 1 ; i <= 4 ; i++){
			IPubRecord rec = pubData.getRecord("RecDesk_" + i);
			if (rec == null) {
				continue;
			}
			for (int j = 0; j < rec.getRows() ; j++){
				int deskId = rec.getInt(j,REC_BOSS_COL.COL_OBJID.ordinal());
				ServerMsg.IntSingle.Builder build = ServerMsg.IntSingle.newBuilder();
				build.setIntMember(deskId);
				kernel.broadToServer("game",ServerMsgDef.P2G_CLOSE_BOSS_BATTLE.ordinal(),build.build().toByteArray());
			}
		}
		return true;
	}
}
