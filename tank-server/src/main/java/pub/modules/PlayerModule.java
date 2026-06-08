/**   
*    
* 描述：   记录玩家离线数据
* 文件：PlayerModule.java
* 创建人：胡中伟
* 创建时间：2018年5月18日 下午5:25:53 
*    
*/
package pub.modules;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.ValueType;
import framework.pub.IPubData;
import framework.pub.IPubKernel;
import framework.pub.IPubModule;
import framework.pub.IPubRecord;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * 
 * 描述：
 * 
 */
public class PlayerModule implements IPubModule {
	static Logger logger = LoggerFactory.getLogger(PlayerModule.class);

	enum RECRUIT_COL {
		COL_UID, COL_TIME,

		COL_END
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IPubKernel kernel) {
		kernel.regServerMsg(ServerMsgDef.PUBMSG_PLAYER_DATA.ordinal(), this, "ChangePlayerData");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_NEW_RECRUIT.ordinal(), this, "onNewRecruit");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_UPDATE_RECHARGE.ordinal(), this, "onUpdateRecharge");
		kernel.regServerRequest(ServerMsgDef.B2P_RECRUIT_DATA.ordinal(), this, "OnGetRecruitData");
		kernel.regServerRequest(ServerMsgDef.B2P_RECRUITER_DATA.ordinal(), this, "OnGetRecruiterData");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_RECRUIT_RECHARGE.ordinal(), this, "OnGetRecruitRecharge");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_RECRUIT_INFO.ordinal(), this, "OnGetRecruitInfo");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	void ChangePlayerData(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.PlayerData msg = ServerMsg.PlayerData.parseFrom(data);
		int uid = msg.getUid();
		String pubdataName = "Player_" + uid;
		IPubData pubdata = kernel.getPubData(pubdataName,true);
		if (msg.hasSex()) {
			AddPro(pubdata, PLAYER_PROPERTY_SEX, ValueType.INT, msg.getSex());
		}
		if (msg.hasName()) {
			AddPro(pubdata, PLAYER_PROPERTY_NAME, ValueType.STRING, msg.getName());
		}
		if (msg.hasSign()) {
			AddPro(pubdata, PLAYER_PROPERTY_SIGN, ValueType.STRING, msg.getSign());
		}
		if (msg.hasTitle()) {
			AddPro(pubdata, PLAYER_PROPERTY_TITLEID, ValueType.STRING, msg.getTitle());
		}
		if (msg.hasLevel()) {
			AddPro(pubdata, PLAYER_PROPERTY_LEVEL, ValueType.INT, msg.getLevel());
		}
		if (msg.hasHeadid()) {
			AddPro(pubdata, PLAYER_PROPERTY_HEADID, ValueType.INT, msg.getHeadid());
		}
		if (msg.hasBet()) {
			AddPro(pubdata, "Bet", ValueType.INT, msg.getBet());
		}
		if (msg.hasScore()) {
			AddPro(pubdata, "Score", ValueType.INT, msg.getScore());
		}
		if (msg.hasSkin()) {
			AddPro(pubdata, "Skin", ValueType.INT, msg.getSkin());
		}
		if (msg.hasMaxScore()) {
			AddPro(pubdata, "MaxScore", ValueType.INT, msg.getMaxScore());
		}
		if (msg.hasHitEggCount()) {
			AddPro(pubdata, PLAYER_PROPERTY_HITEGGCOUNT, ValueType.INT, msg.getHitEggCount());
		}
		kernel.storePubData(pubdata);
	}

	void AddPro(IPubData pubdata, String name, ValueType type, Object val) {
		if (pubdata.getValue(name) == null) {
			pubdata.addProperty(name, type, val, true);
		} else {
			pubdata.setValue(name, val);
		}
	}

	public void onNewRecruit(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.AddRecruit msg = ServerMsg.AddRecruit.parseFrom(data);
		String recruiter = "Player_" + msg.getRecruitor();
		IPubData pubdata = kernel.getPubData(recruiter,true);
		IPubRecord rec = pubdata.getRecord("Friends");
		if (rec == null) {
			rec = pubdata.addRecord("Friends", 2, 200, true);
			rec.setColType(RECRUIT_COL.COL_UID.ordinal(), ValueType.INT);
			rec.setColType(RECRUIT_COL.COL_TIME.ordinal(), ValueType.LONG);
		}
		rec.addRow(msg.getBeRecruitor(), kernel.getServerTime());
		if (pubdata.getValue("FriendAmount") == null) {
			pubdata.addProperty("FriendAmount", ValueType.INT, 1, true);
		} else {
			pubdata.setValue("FriendAmount", (int) pubdata.getValue("FriendAmount") + 1);
		}
		kernel.storePubData(pubdata);
	}

	public void onUpdateRecharge(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.UpdateRecharge msg = ServerMsg.UpdateRecharge.parseFrom(data);
		String pubdataName = "Player_" + msg.getUid();
		IPubData pubdata = kernel.getPubData(pubdataName,true);
		boolean sameWeek = true;
		boolean inOneWeek = true;
		if (pubdata.getValue("LastRecharge") == null) {
			pubdata.addProperty("LastRecharge", ValueType.LONG, kernel.getServerTime(), true);
		} else {
			long lastRecharge = (long) pubdata.getValue("LastRecharge");
			pubdata.setValue("LastRecharge", kernel.getServerTime());
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTimeInMillis(lastRecharge);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTimeInMillis(kernel.getServerTime());
			if (calendar1.get(Calendar.WEEK_OF_YEAR) != calendar2.get(Calendar.WEEK_OF_YEAR)) {
				sameWeek = false;
				if (UtilFunc.getZeroTime(calendar2.getTimeInMillis())
						- UtilFunc.getZeroTime(calendar1.getTimeInMillis()) > 604800000) {// 超过一周
					inOneWeek = false;
				}
			}
		}
		if (pubdata.getValue("WeekRecharge") == null) {
			pubdata.addProperty("WeekRecharge", ValueType.INT, msg.getPrice(), true);
			pubdata.addProperty("LastWeekRecharge", ValueType.INT, msg.getPrice(), true);
		} else {
			if (sameWeek) {
				pubdata.setValue("WeekRecharge", (int) pubdata.getValue("WeekRecharge") + msg.getPrice());
			} else {
				if (inOneWeek) {
					pubdata.setValue("LastWeekRecharge", pubdata.getValue("WeekRecharge"));
				} else {
					pubdata.setValue("LastWeekRecharge", 0);
				}
				pubdata.setValue("WeekRecharge", msg.getPrice());
			}
		}
		kernel.storePubData(pubdata);
	}

	public void OnGetRecruitData(IPubKernel kernel, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.GetRecruit recruit = ServerMsg.GetRecruit.parseFrom(data);
		String recruiter = "Player_" + recruit.getUid();
		IPubData pubdata = kernel.getPubData(recruiter,false);
		ServerMsg.RecruitDatas.Builder build = ServerMsg.RecruitDatas.newBuilder();
		if (pubdata == null) {
			kernel.responseServer(msgid, build.build().toByteArray());
			return;
		}
		IPubRecord rec = pubdata.getRecord("Friends");
		if (rec != null) {
			int rows = rec.getRows();
			for (int i = 0; i < rows; i++) {
				int uid = rec.getInt(i, RECRUIT_COL.COL_UID.ordinal());
				long recruitTime = rec.getLong(i, RECRUIT_COL.COL_TIME.ordinal());
				if (recruitTime < recruit.getFrom() || recruitTime > recruit.getTo())
					continue;
				ServerMsg.RecruitData.Builder info = ServerMsg.RecruitData.newBuilder();
				info.setUid(uid);
				info.setRecruitTime(recruitTime);
				String beRecruiter = "Player_" + uid;
				pubdata = kernel.getPubData(beRecruiter,false);
				int friendAmount = 0;
				if (pubdata != null) {
					Object o = pubdata.getValue("FriendAmount");
					if (o != null) {
						friendAmount = (int) pubdata.getValue("FriendAmount");
					}
				}
				info.setFriendAmount(friendAmount);
				Object lastRecharge = null;
				if (pubdata == null || (lastRecharge = pubdata.getValue("LastRecharge")) == null) {
					info.setWeekRecharge(0);
				} else {
					Calendar calendar1 = Calendar.getInstance();
					calendar1.setTimeInMillis((long) lastRecharge);
					Calendar calendar2 = Calendar.getInstance();
					calendar2.setTimeInMillis(kernel.getServerTime());
					if (calendar1.get(Calendar.WEEK_OF_YEAR) != calendar2.get(Calendar.WEEK_OF_YEAR)) {
						info.setWeekRecharge(0);
					} else {
						info.setWeekRecharge((int) pubdata.getValue("WeekRecharge"));
					}
				}
				build.addRecruitDatas(info.build());
			}
		}
		kernel.responseServer(msgid, build.build().toByteArray());
	}

	public void OnGetRecruiterData(IPubKernel kernel, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.GetRecruiter recruiter = ServerMsg.GetRecruiter.parseFrom(data);
		String recruiterP = "Player_" + recruiter.getUid();
		IPubData pubdata = kernel.getPubData(recruiterP,false);
		ServerMsg.RecruiterData.Builder build = ServerMsg.RecruiterData.newBuilder();
		int friendAmount = 0;
		int weekRecharge = 0;
		build.setFriendAmount(friendAmount);
		build.setWeekRecharge(weekRecharge);
		if (pubdata == null) {
			kernel.responseServer(msgid, build.build().toByteArray());
			return;
		}
		Object o = pubdata.getValue("FriendAmount");
		if (o != null) {
			friendAmount = (int) o;
		}
		Object lastRecharge = null;
		if ((lastRecharge = pubdata.getValue("LastRecharge")) != null) {
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTimeInMillis((long) lastRecharge);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTimeInMillis(kernel.getServerTime());
			if (calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR)) {
				weekRecharge = (int) pubdata.getValue("WeekRecharge");
			}
		}
		build.setFriendAmount(friendAmount);
		build.setWeekRecharge(weekRecharge);
		kernel.responseServer(msgid, build.build().toByteArray());
	}

	void OnGetRecruitRecharge(IPubKernel kernel, int reqid, byte[] data) {
		int uid = (data[0] << 24 & 0xFF000000) | (data[1] << 16 & 0xFF0000) | (data[2] << 8 & 0xFF00)
				| (data[3] & 0xFF);
		ServerMsg.IntSingle.Builder builder = ServerMsg.IntSingle.newBuilder();
		builder.setIntMember(0);
		String recruiter = "Player_" + uid;
		IPubData pubdata = kernel.getPubData(recruiter,false);
		if (pubdata == null){
			kernel.responseServer(reqid, builder.build().toByteArray());
			return ;
		}
		IPubRecord rec = pubdata.getRecord("Friends");
		if (rec != null) {
			int rows = rec.getRows();
			int friendRecharge = 0;
			int fansRecharge = 0;
			for (int i = 0; i < rows; i++) {
				int beUid = rec.getInt(i, RECRUIT_COL.COL_UID.ordinal());
				String beRecruiter = "Player_" + beUid;
				pubdata = kernel.getPubData(beRecruiter,false);
				if (pubdata != null) {
					// 好友充值总额
					friendRecharge += GetLastRecharge(pubdata, kernel);
					// 粉丝充值总额
					IPubRecord fansRec = pubdata.getRecord("Friends");
					if (fansRec != null) {
						int fansRows = fansRec.getRows();
						for (int j = 0; j < fansRows; j++) {
							int fansUid = fansRec.getInt(j, RECRUIT_COL.COL_UID.ordinal());
							String fans = "Player_" + fansUid;
							pubdata = kernel.getPubData(fans,false);
							if (pubdata != null) {
								fansRecharge += GetLastRecharge(pubdata, kernel);
							}
						}
					}
				}
			}
			int exchangeCard = 0;
			if (friendRecharge <= 5000) {
				exchangeCard += (int) (friendRecharge * 0.1);
			} else if (friendRecharge > 20000) {
				exchangeCard += (int) (friendRecharge * 0.15);
			} else {
				exchangeCard += (int) (friendRecharge * 0.12);
			}
			if (fansRecharge <= 5000) {
				exchangeCard += (int) (fansRecharge * 0.01);
			} else if (fansRecharge > 20000) {
				exchangeCard += (int) (fansRecharge * 0.03);
			} else {
				exchangeCard += (int) (fansRecharge * 0.02);
			}
			builder.setIntMember(exchangeCard);
			long weekBegin = UtilFunc.weekBegin(kernel.getServerTime() - 604800000);
			long weekEnd = weekBegin + 604799000;
			String s1 = kernel.getServer().getTimeFormat().format(weekBegin).split(" ")[0];
			String s2 = kernel.getServer().getTimeFormat().format(weekEnd).split(" ")[0];
			String statsTime = s1 + " > " + s2;
			kernel.cardStats(statsTime, uid, rows, friendRecharge, fansRecharge, exchangeCard, (Boolean res) -> { });
		}
		kernel.responseServer(reqid, builder.build().toByteArray());
	}

	private int GetLastRecharge(IPubData pubdata, IPubKernel kernel) {
		Object lastRecharge = null;
		int recharge = 0;
		if ((lastRecharge = pubdata.getValue("LastRecharge")) != null) {
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTimeInMillis((long) lastRecharge);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTimeInMillis(kernel.getServerTime());
			if (calendar1.get(Calendar.WEEK_OF_YEAR) != calendar2.get(Calendar.WEEK_OF_YEAR)) {
				if (UtilFunc.getZeroTime(calendar2.getTimeInMillis())
						- UtilFunc.getZeroTime(calendar1.getTimeInMillis()) <= 604800000) {// 一周内
					recharge = (int) pubdata.getValue("WeekRecharge");
				}
			} else {// 最后一笔充值在本周，有可能上周的充值额已经单独存储
				recharge = (int) pubdata.getValue("LastWeekRecharge");
			}
		}
		return recharge;
	}

	void OnGetRecruitInfo(IPubKernel kernel, int reqid, byte[] data) {
		int uid = (data[0] << 24 & 0xFF000000) | (data[1] << 16 & 0xFF0000) | (data[2] << 8 & 0xFF00)
				| (data[3] & 0xFF);
		String recruiter = "Player_" + uid;
		IPubData pubdata = kernel.getPubData(recruiter,false);
		int friendRecharge = 0;
		int fansRecharge = 0;
		int friendCard = 0;
		int fansCard = 0;
		int friendAmount = 0;
		ServerMsg.RecruitInfo.Builder builder = ServerMsg.RecruitInfo.newBuilder();
		if (pubdata != null){
			IPubRecord rec = pubdata.getRecord("Friends");
			Object o = pubdata.getValue("FriendAmount");
			if (o != null) {
				friendAmount = (int) pubdata.getValue("FriendAmount");
			}
			if (rec != null) {
				int rows = rec.getRows();
				for (int i = 0; i < rows; i++) {
					int beUid = rec.getInt(i, RECRUIT_COL.COL_UID.ordinal());
					String beRecruiter = "Player_" + beUid;
					pubdata = kernel.getPubData(beRecruiter,false);
					if (pubdata != null) {
						// 好友充值总额
						friendRecharge += GetRecharge(pubdata, kernel);
						// 粉丝充值总额
						IPubRecord fansRec = pubdata.getRecord("Friends");
						if (fansRec != null) {
							int fansRows = fansRec.getRows();
							for (int j = 0; j < fansRows; j++) {
								int fansUid = fansRec.getInt(j, RECRUIT_COL.COL_UID.ordinal());
								String fans = "Player_" + fansUid;
								pubdata = kernel.getPubData(fans,false);
								if (pubdata != null) {
									fansRecharge += GetRecharge(pubdata, kernel);
								}
							}
						}
					}
				}
				if (friendRecharge <= 5000) {
					friendCard += (int) (friendRecharge * 0.1);
				} else if (friendRecharge > 20000) {
					friendCard += (int) (friendRecharge * 0.15);
				} else {
					friendCard += (int) (friendRecharge * 0.12);
				}
				if (fansRecharge <= 5000) {
					fansCard += (int) (fansRecharge * 0.01);
				} else if (fansRecharge > 20000) {
					fansCard += (int) (fansRecharge * 0.03);
				} else {
					fansCard += (int) (fansRecharge * 0.02);
				}
			}
		}
		builder.setFriendAmount(friendAmount);
		builder.setFriendRecharge(friendRecharge);
		builder.setFansRecharge(fansRecharge);
		builder.setFriendCard(friendCard);
		builder.setFansCard(fansCard);
		kernel.responseServer(reqid, builder.build().toByteArray());
	}

	private int GetRecharge(IPubData pubdata, IPubKernel kernel) {
		Object lastRecharge = null;
		int recharge = 0;
		if ((lastRecharge = pubdata.getValue("LastRecharge")) != null) {
			Calendar calendar1 = Calendar.getInstance();
			calendar1.setTimeInMillis((long) lastRecharge);
			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTimeInMillis(kernel.getServerTime());
			if (calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR)) {
				recharge = (int) pubdata.getValue("WeekRecharge");
			}
		}
		return recharge;
	}
}
