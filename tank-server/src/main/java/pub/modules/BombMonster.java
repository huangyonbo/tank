package pub.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ICfgReader;
import framework.game.ValueType;
import framework.pub.*;
import game.custommsg.ServerCodeDef;
import game.util.TimeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BombMonster implements IPubModule {
	private final String PUB_SPACE = "pubdata";
	private final String PUB_DATA = "BombMonsterData";
	private final String PUB_PROPERTY_ID = "MonsterId";
	private final String PUB_PROPERTY_BLOOD = "MonsterBlood";
	private final String PUB_PROPERTY_REFRESH_TIME = "MonsterRefreshTime";
	private final String PUB_PROPERTY_COLOR_TICKET_AMOUNT = "MonsterAmount";// 今日已经获得彩券数量
	private final String PUB_PROPERTY_COLOR_TICKET_LAST_TIME = "MonsterLastTime";// 上次获得彩券时间
	private final String PUB_PROPERTY_TOTAL_RECHARGE = "MonsterTotalRecharge";// 今日充值总额
	private final String PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME = "MonsterTotalRechargeLastTime";// 上次充值时间
	private final String PUB_PROPERTY_VERSION = "BombMonsterVersion";
	private final String PUB_RECORD_AWARD = "MonsterAwardRec";
	private Random random = new Random();
	private Map<Integer, Monster> m_mapMonster = new HashMap<>();

	class Monster {
		int id;
		int rate;
		int minBlood;
		int maxBlood;
	}

	@Override
	public boolean onInit(IPubKernel kernel) {
		kernel.regServerMsg(ServerMsgDef.PUBMSG_REFRESH_MONSTER.ordinal(), this, "onRefreshMonster");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_MONSTER_TOTAL_RECHARGE.ordinal(), this, "onAddTotalRecharge");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_BOMB_MONSTER_CHECK_VER.ordinal(), this, "onCheckVer");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_BOMB_MONSTER.ordinal(), this, "onBombMonster");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_ON_MONSTER_AWARD_SHOW.ordinal(), this, "OnAddMonsterAwardShow");
		kernel.regOnLoadEvent("pubdata", this, "onPubdataLoad");
		RefreshCfg(kernel, "res/Activity/BombMonsterConfig.xml");
		return true;
	}

	@Override
	public void onDestroy() {

	}

	void RefreshCfg(IPubKernel kernel, String path) {
		if (path.equals("res/Activity/BombMonsterConfig.xml")) {
			ICfgReader cfg = kernel.loadXmlConfig(path);
			if (cfg == null) {
				return;
			}
			int count = cfg.getItemCount();
			for (int i = 0; i < count; ++i) {
				Monster monster = new Monster();
				monster.id = cfg.getInt(i, "Id");
				monster.rate = cfg.getInt(i, "Rate");
				String[] bloods = cfg.getString(i, "Blood").split("-");
				monster.minBlood = Integer.parseInt(bloods[0]);
				monster.maxBlood = Integer.parseInt(bloods[1]);
				m_mapMonster.put(monster.id, monster);
			}
		}
	}

	public void onRefreshMonster(IPubKernel kernel, int serid, int msgid, byte[] data) {
		refreshMonster(kernel);
	}

	public void onAddTotalRecharge(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.IntSingle msg = ServerMsg.IntSingle.parseFrom(data);
		IPubData pubData = getPubData(kernel);
		if (pubData == null) {
			return;
		}
		long current = kernel.getServerTime();
		if (!TimeUtils.isSameDay((long) pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME), current)) {
			pubData.setValue(PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME, current);
			pubData.setValue(PUB_PROPERTY_TOTAL_RECHARGE, 0l);
		}
		pubData.setValue(PUB_PROPERTY_TOTAL_RECHARGE,
				(long) pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE) + msg.getIntMember());
	}

	public void onBombMonster(IPubKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.BombMonsterResult.Builder build = ServerMsg.BombMonsterResult.newBuilder();
		ServerMsg.BombMonster msg = ServerMsg.BombMonster.parseFrom(data);
		IPubData pubData = getPubData(kernel);
		if (pubData == null) {
			return;
		}
		int blood = 0;
		int id = 0;
		if ((id = (int) pubData.getValue(PUB_PROPERTY_ID)) == 0
				|| (blood = (int) pubData.getValue(PUB_PROPERTY_BLOOD)) == 0) {
			build.setCode(ServerCodeDef.CODE_WRONG_STATE.ordinal());
			build.setMonster(id);
			kernel.responseServer(reqid, build.build().toByteArray());
			return;
		}
		if (random.nextFloat() <= (float) msg.getBombValue() * 0.9f / blood) {// 打死了
			pubData.setValue(PUB_PROPERTY_ID, 0);
			pubData.setValue(PUB_PROPERTY_BLOOD, 0);
			long current = kernel.getServerTime();
			long refresh = current + 30000;
			pubData.setValue(PUB_PROPERTY_REFRESH_TIME, refresh);
			build.setCode(ServerCodeDef.CODE_IS_DIED.ordinal());
			build.setRefreshTime(refresh);
			if (!TimeUtils.isSameDay((long) pubData.getValue(PUB_PROPERTY_COLOR_TICKET_LAST_TIME), current)) {
				pubData.setValue(PUB_PROPERTY_COLOR_TICKET_AMOUNT, 0l);
			}
			long colorTicketAmount = (long) pubData.getValue(PUB_PROPERTY_COLOR_TICKET_AMOUNT);
			long amount = colorTicketAmount + msg.getColorTicket();
			if (msg.getDailyLimit() == -1 || amount < msg.getDailyLimit()) {
				if (!TimeUtils.isSameDay((long) pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME), current)) {
					pubData.setValue(PUB_PROPERTY_TOTAL_RECHARGE, 0l);
				}
				if (((long) pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE) * 100 / 6 - colorTicketAmount) >= msg
						.getColorTicket()) {
					build.setColorTicket(true);
					pubData.setValue(PUB_PROPERTY_COLOR_TICKET_AMOUNT, amount);
					pubData.setValue(PUB_PROPERTY_COLOR_TICKET_LAST_TIME, current);
				}
			}
		} else {
			build.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
		}
		build.setMonster(id);
		kernel.responseServer(reqid, build.build().toByteArray());
	}

	public void onCheckVer(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.Int64 msg = ServerMsg.Int64.parseFrom(data);
		IPubData pubData = getPubData(kernel);
		if (pubData == null) {
			return;
		}
		if ((long) pubData.getValue(PUB_PROPERTY_VERSION) != msg.getValue()) {
			pubData.setValue(PUB_PROPERTY_VERSION, msg.getValue());
			pubData.setValue(PUB_PROPERTY_COLOR_TICKET_AMOUNT, 0l);
			pubData.setValue(PUB_PROPERTY_TOTAL_RECHARGE, 0l);
			pubData.getRecord(PUB_RECORD_AWARD).clear();
			refreshMonster(kernel);
		}
	}

	public void onPubdataLoad(IPubKernel kernel, String none) {
		ensurePropertyNotNull(kernel);
	}

	private void refreshMonster(IPubKernel kernel) {
		int rate = random.nextInt(101);
		int tRate = 0;
		Monster monster = null;
		for (Map.Entry<Integer, Monster> entry : m_mapMonster.entrySet()) {
			tRate += entry.getValue().rate;
			if (rate <= tRate) {
				monster = entry.getValue();
				break;
			}
		}
		if (monster == null) {
			return;
		}
		IPubData pubData = getPubData(kernel);
		if (pubData == null){
			return;
		}
		pubData.setValue(PUB_PROPERTY_ID, monster.id);
		int blood = monster.minBlood + random.nextInt(monster.maxBlood - monster.minBlood + 1);
		pubData.setValue(PUB_PROPERTY_BLOOD, blood);
		pubData.setValue(PUB_PROPERTY_REFRESH_TIME, 0l);
	}


	private IPubData ensurePropertyNotNull(IPubKernel kernel) {
		IPubData pubData = kernel.getPubData(PUB_DATA,false);
		if (pubData != null){
			return pubData;
		}
		pubData = new PubData(PUB_DATA);
		if (pubData.getValue(PUB_PROPERTY_ID) == null) {
			pubData.addProperty(PUB_PROPERTY_ID, ValueType.INT, 0, true);
		}
		if (pubData.getValue(PUB_PROPERTY_BLOOD) == null) {
			pubData.addProperty(PUB_PROPERTY_BLOOD, ValueType.INT, 0, true);
		}
		if (pubData.getValue(PUB_PROPERTY_REFRESH_TIME) == null) {
			pubData.addProperty(PUB_PROPERTY_REFRESH_TIME, ValueType.LONG, 0l, true);
		}
		if (pubData.getValue(PUB_PROPERTY_COLOR_TICKET_AMOUNT) == null) {
			pubData.addProperty(PUB_PROPERTY_COLOR_TICKET_AMOUNT, ValueType.LONG, 0l, true);
		}
		if (pubData.getValue(PUB_PROPERTY_COLOR_TICKET_LAST_TIME) == null) {
			pubData.addProperty(PUB_PROPERTY_COLOR_TICKET_LAST_TIME, ValueType.LONG, 0l, true);
		}
		if (pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE) == null) {
			pubData.addProperty(PUB_PROPERTY_TOTAL_RECHARGE, ValueType.LONG, 0l, true);
		}
		if (pubData.getValue(PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME) == null) {
			pubData.addProperty(PUB_PROPERTY_TOTAL_RECHARGE_LAST_TIME, ValueType.LONG, 0l, true);
		}
		if (pubData.getValue(PUB_PROPERTY_VERSION) == null) {
			pubData.addProperty(PUB_PROPERTY_VERSION, ValueType.LONG, 0l, true);
		}
		IPubRecord rec = pubData.getRecord(PUB_RECORD_AWARD);
		if (rec == null) {
			rec = pubData.addRecord(PUB_RECORD_AWARD, 6, 10, true);
			rec.setColType(0, ValueType.INT);// 奖励类型
			rec.setColType(1, ValueType.INT);// uid
			rec.setColType(2, ValueType.STRING);// 使用弹头id
			rec.setColType(3, ValueType.STRING);// 奖励id
			rec.setColType(4, ValueType.INT);// 奖励数量
			rec.setColType(5, ValueType.STRING);// username
		}
		kernel.storePubData(pubData);
		return pubData;
	}

	private IPubData getPubData(IPubKernel kernel) {
		return kernel.getPubData(PUB_DATA,false);
	}

	public void OnAddMonsterAwardShow(IPubKernel kernel, int reqid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.MonsterAwardRecord awardInfo = ServerMsg.MonsterAwardRecord.parseFrom(data);
		IPubData pubData = getPubData(kernel);
		if (pubData == null){
			kernel.responseServer(reqid, new byte[] {});
			return;
		}
		IPubRecord rec = pubData.getRecord(PUB_RECORD_AWARD);
		if (rec.getRows() >= 10) {
			rec.removeRow(0);
		}
		rec.addRow(awardInfo.getType(), awardInfo.getUid(), awardInfo.getItemId(), awardInfo.getPkgId(),
				awardInfo.getCount(), awardInfo.getUsername());
		kernel.responseServer(reqid, new byte[] {});
	}
}
