/**   
*    
* 描述：   
* 文件：PbuModule.java
* 创建人：胡中伟
* 创建时间：2018年4月11日 下午8:23:19 
*    
*/
package pub.modules;

import back.modules.dataenum.NoticeType;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.JsonUtil;
import framework.game.ValueType;
import framework.pub.*;
import game.modules.GMModule;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * 描述：
 * 
 */
public class PubModule implements IPubModule {

	private static Logger logger = LoggerFactory.getLogger(PubModule.class);

	enum PLAYER_COUNT_COL {
		COL_ROOM_ID, COL_PLAYER_COUNT, COL_VIRTUAL_COUNT, COL_REFRESH_TIME,

		COL_END
	}

	private static Map<Integer, Integer> channelCurrentUserCountMap = new HashMap<>();

	private static Map<Integer, Integer> channelMaxUserCountMap = new HashMap<>();

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IPubKernel kernel) {
		kernel.regOnLoadEvent("pubdata", this, "OnPubdataLoad");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_ROOM.ordinal(), this, "OnAddRoomPubData");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_SET_TOTALPW.ordinal(), this, "OnSetRoomTotalPW");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ENTER_ROOM.ordinal(), this, "OnAddPlayerCount");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_LEAVE_ROOM.ordinal(), this, "OnSubPlayerCount");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_UPDATE_TOTALPW.ordinal(), this, "OnUpdateDayPW");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_STORE_FISH_POND_ROBOT_PW.ordinal(), this, "OnStoreFishPondRobotPW");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ADD_FISH_POND_PUB_DATA.ordinal(), this, "OnAddFishPondPubData");

//		kernel.RegServerMsg(ServerMsgDef.PUBMSG_UPDATE_MOJINRECORD.ordinal(), this, "OnStoreMojinRoomRecord");

		kernel.regServerMsg(ServerMsgDef.PUBMSG_MAINTAIN.ordinal(), this, "OnBackMaintain");
		kernel.regServerMsg(ServerMsgDef.B2P_PUBNOTICE.ordinal(), this, "OnBackPubNotice");
		kernel.regServerMsg(ServerMsgDef.B2P_REPNOTICE.ordinal(), this, "OnBackRepNotice");
		kernel.regServerMsg(ServerMsgDef.B2P_FEEDBACK_SERVICE.ordinal(), this, "OnBackFeedbackService");
		kernel.regServerMsg(ServerMsgDef.B2P_ALLROOM_DATA.ordinal(), this, "OnRecvAllRoomRunInfo");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_INIT_PLAYER_COUNT.ordinal(), this, "OnInitPlayerCount");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_PLAYER_LOGIN.ordinal(), this, "OnPlayerLogin");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_PLAYER_LOGOUT.ordinal(), this, "OnPlayerLogout");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ON_CHANGE_DAY.ordinal(), this, "OnChangeDay");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ON_HIT_EGG_AWARD_CHANGE.ordinal(), this, "OnHitEggAwardChange");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ON_ADD_HIT_EGG_AWARD_SHOW.ordinal(), this, "OnAddHitEggAwardShow");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_ON_CLEAR_HIT_EGG_AWARD_VALUE.ordinal(), this, "OnClearHitEggAwardValue");
		kernel.regServerMsg(ServerMsgDef.PUBMSG_GOLD_EGG_CHECK_VER.ordinal(), this, "OnCheckGoldEggVer");
		kernel.regServerRequest(ServerMsgDef.PUBMSG_UPDATE_DATA.ordinal(), this, "OnUpdatePublicData");
		return true;
	}

	public void OnInitPlayerCount(IPubKernel kernel, int serid, int msgid, byte[] data) {
		IPubData pubData = kernel.getPubData("RoomData",false);
		IPubRecord rec   = pubData.getRecord("PlayerCount");
		for (int i = 0; i <= 3 ; i++) {
			rec.addRow(i, 0, getVirtualCount(i), kernel.getServerTime());
		}
		kernel.storePubData(pubData);
	}

	public void OnPlayerLogin(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PlayerStatisticsInfo playerInfo = ServerMsg.PlayerStatisticsInfo.parseFrom(data);
		int channel = playerInfo.getChannel();
		if (channelCurrentUserCountMap.containsKey(channel)) {
			channelCurrentUserCountMap.put(channel, channelCurrentUserCountMap.get(channel) + 1);
		} else {
			channelCurrentUserCountMap.put(channel, 1);
		}
		int currentCount = channelCurrentUserCountMap.get(channel);
		if (channelMaxUserCountMap.containsKey(channel)) {
			int maxCount = channelMaxUserCountMap.get(channel);
			if (maxCount < currentCount) {
				channelMaxUserCountMap.put(channel, currentCount);
			}
		} else {
			channelMaxUserCountMap.put(channel, currentCount);
		}
	}

	public void OnPlayerLogout(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PlayerStatisticsInfo playerInfo = ServerMsg.PlayerStatisticsInfo.parseFrom(data);
		int channel = playerInfo.getChannel();
		if (channelCurrentUserCountMap.containsKey(channel)) {
			int currentCount = channelCurrentUserCountMap.get(channel);
			currentCount--;
			currentCount = currentCount <= 0 ? 0 : currentCount;
			channelCurrentUserCountMap.put(channel, currentCount);
		}
	}

	public void OnChangeDay(IPubKernel pubKernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		channelMaxUserCountMap.forEach((k, v) -> {
			logger.info("Channel : " + k + " Count : " + v);
			pubKernel.updateOnlineCount(k, v);
			channelMaxUserCountMap.put(k, channelCurrentUserCountMap.get(k));
		});
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {

	}

	void OnAddRoomPubData(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubAddRoom addRoom = ServerMsg.PubAddRoom.parseFrom(data);
		IPubData pubData = createRoomPubData(kernel, addRoom.getName());
		kernel.storePubData(pubData);
	}

	private IPubData createRoomPubData(IPubKernel kernel,  String name) {
		IPubData pubData = kernel.getPubData(name, true);
		pubData.addProperty(PLAYER_PROPERTY_TOTALPLAY, ValueType.LONG, 0L, true);
		pubData.addProperty(PLAYER_PROPERTY_TOTALWIN, ValueType.LONG, 0L, true);
		pubData.addProperty("TodayPlay", ValueType.LONG, 0L, true);
		pubData.addProperty("TodayWin", ValueType.LONG, 0L, true);
		long now = UtilFunc.getZeroTime(kernel.getServerTime());
		pubData.addProperty("TodayDate", ValueType.LONG, now, true);
		return pubData;
	}

	void OnSetRoomTotalPW(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubSetRoomTotalPW totalpw = ServerMsg.PubSetRoomTotalPW.parseFrom(data);
		IPubData pubData = kernel.getPubData(totalpw.getName(), false);
		if (pubData == null||pubData.getPropertiesCount()==0) {
			pubData = createRoomPubData(kernel, totalpw.getName());
		}
		pubData.setValue(PLAYER_PROPERTY_TOTALPLAY, totalpw.getPlay());
		pubData.setValue(PLAYER_PROPERTY_TOTALWIN, totalpw.getWin());
		kernel.storePubData(pubData);
	}

	void OnUpdateDayPW(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.PubUpdateDayPW daypw = ServerMsg.PubUpdateDayPW.parseFrom(data);
		IPubData pubData = kernel.getPubData(daypw.getName(),true);
		long date = daypw.getDate();
		long play = daypw.getPlay();
		long win  = daypw.getWin();
		long last = pubData.getLong("TodayDate");
		if (date != last) {
			// 存库，重置
			String ser = daypw.getSerName();
			int type = daypw.getRoomType();
			kernel.addDayPlayWin(ser, type, last, play, win);
			pubData.setValue("TodayPlay", 0L);
			pubData.setValue("TodayWin", 0L);
			pubData.setValue("TodayDate", date);
		} else {
			pubData.setValue("TodayPlay", play);
			pubData.setValue("TodayWin", win);
		}
		kernel.storePubData(pubData);
	}

	void OnStoreFishPondRobotPW(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
		ServerMsg.StringSingle tmp = ServerMsg.StringSingle.parseFrom(data);
		Map<String, String> map = JsonUtil.decodeToMap(tmp.getWords(), String.class, String.class);
		if (map == null) {
			return;
		}
		IPubData pubData = kernel.getPubData(map.get("pubDataName"),true);
		long totalPlayJinBi = Long.parseLong(map.get("totalPlayJinBi"));
		long totalWinJinBi = Long.parseLong(map.get("totalWinJinBi"));
		long totalPlayMoJing = Long.parseLong(map.get("totalPlayMoJing"));
		long totalWinMoJing = Long.parseLong(map.get("totalWinMoJing"));

		pubData.setValue("totalPlayJinBi", totalPlayJinBi);
		pubData.setValue("totalWinJinBi", totalWinJinBi);
		pubData.setValue("totalPlayMoJing", totalPlayMoJing);
		pubData.setValue("totalWinMoJing", totalWinMoJing);

		kernel.storePubData(pubData);
	}

	void OnAddFishPondPubData(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.PubAddRoom addRoom = ServerMsg.PubAddRoom.parseFrom(data);
		IPubData pubData = kernel.getPubData(addRoom.getName(),true);
		pubData.addProperty("totalPlayJinBi", ValueType.LONG, 0L, true);
		pubData.addProperty("totalWinJinBi", ValueType.LONG, 0L, true);
		pubData.addProperty("totalPlayMoJing", ValueType.LONG, 0L, true);
		pubData.addProperty("totalWinMoJing", ValueType.LONG, 0L, true);
		kernel.storePubData(pubData);
	}

//	void OnStoreMojinRoomRecord(IPubKernel kernel, int serid, int msgid, byte[] data) throws InvalidProtocolBufferException {
//		CustomMsg.String record = CustomMsg.String.parseFrom(data);
//		String datas = record.getValue();
//		JsonObject json = JsonUtil.decodeToObj(datas, JsonObject.class);
//		String pubname = json.get("pubdataname").getAsString();
//		if (pubname == null) {
//			return;
//		}
//		IPubSpace pubSpace = kernel.GetPubSpace("pubdata");
//		IPubData pubData = pubSpace.GetPubData(pubname);
//		if (pubData == null) {
//			return;
//		}
//		pubData.SetValue("MojinRoom",json.get("MojinRoom").getAsString());
//		pubData.SetValue("TodayDate", UtilFunc.GetZeroTime(kernel.GetServerTime()));
//	}


	public void OnAddPlayerCount(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.UpdateRoom update = ServerMsg.UpdateRoom.parseFrom(data);
		int type = update.getRoomid();
		IPubData pubData = kernel.getPubData("RoomData",false);
		IPubRecord rec = pubData.getRecord("PlayerCount");
		if (rec == null) {
			logger.error("Error !!! Record PlayerCount is null");
			return;
		}
		/*
		 * if (rec == null) { rec = pubData.AddRecord("PlayerCount", 4, 10,
		 * false);//增加虚拟人数数据和虚拟人数数据刷新时间
		 * rec.SetColType(PLAYER_COUNT_COL.COL_ROOM_ID.ordinal(),
		 * ValueType.VTYPE_INT);
		 * rec.SetColType(PLAYER_COUNT_COL.COL_PLAYER_COUNT.ordinal(),
		 * ValueType.VTYPE_INT);
		 * rec.SetColType(PLAYER_COUNT_COL.COL_VIRTUAL_COUNT.ordinal(),
		 * ValueType.VTYPE_INT);
		 * rec.SetColType(PLAYER_COUNT_COL.COL_REFRESH_TIME.ordinal(),
		 * ValueType.VTYPE_LONG); }
		 */
		int row = rec.findRow(0, 0, type);
		if (row == -1) {
			rec.addRow(type, 1, getVirtualCount(type), kernel.getServerTime());
		} else {
			long lastRefreshTime = rec.getLong(row, 3);
			if (kernel.getServerTime() - lastRefreshTime >= 60 * 60 * 1000) {
				rec.setValue(row, PLAYER_COUNT_COL.COL_PLAYER_COUNT.ordinal(), rec.getInt(row, 1) + 1);
				rec.setValue(row, PLAYER_COUNT_COL.COL_VIRTUAL_COUNT.ordinal(), getVirtualCount(type));
				rec.setValue(row, PLAYER_COUNT_COL.COL_REFRESH_TIME.ordinal(), kernel.getServerTime());
			} else {
				rec.setValue(row, 1, rec.getInt(row, 1) + 1);
			}
		}
		kernel.storePubData(pubData);
	}

	private int getVirtualCount(int type) {
		Random rand = new Random();
		if (type == 3) {
			// 第4个房间设定为在二位数之间随机，随机范围10-99
			return rand.nextInt(90) + 10;
		} else {
			return rand.nextInt((int) Math.pow(10, 3 - type) + 10) + (3 - type) * 1000 + (3 - type) * 100;
		}
	}

	public void OnSubPlayerCount(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.UpdateRoom update = ServerMsg.UpdateRoom.parseFrom(data);
		int type = update.getRoomid();
		IPubData pubData = kernel.getPubData("RoomData",false);
		IPubRecord rec = pubData.getRecord("PlayerCount");
		if (rec == null) {
			return;
		}
		int row = rec.findRow(0, 0, type);
		if (row == -1) {
			return;
		}
		long lastRefreshTime = rec.getLong(row, 3);
		if (kernel.getServerTime() - lastRefreshTime >= 60 * 60 * 1000) {
			rec.setValue(row, PLAYER_COUNT_COL.COL_PLAYER_COUNT.ordinal(), rec.getInt(row, 1) - 1);
			rec.setValue(row, PLAYER_COUNT_COL.COL_VIRTUAL_COUNT.ordinal(), getVirtualCount(type));
			rec.setValue(row, PLAYER_COUNT_COL.COL_REFRESH_TIME.ordinal(), kernel.getServerTime());
		} else {
			rec.setValue(row, 1, rec.getInt(row, 1) - 1);
		}
		kernel.storePubData(pubData);
	}

	public void OnBackMaintain(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		IPubData pubData = kernel.getPubData("PubMaintainData",true);
		IPubRecord rec   = pubData.getRecord("PubMaintainTable");
		ServerMsg.BackMaintainMsg backMaintain = ServerMsg.BackMaintainMsg.parseFrom(data);
		int operation = backMaintain.getOperation();
		int id = backMaintain.getId();
		if (operation == 1) {
			if (rec == null) {
				rec = pubData.addRecord("PubMaintainTable",7,10,true);
				rec.setColType(0, ValueType.INT);
				rec.setColType(1, ValueType.INT);
				rec.setColType(2, ValueType.STRING);//渠道id,逗号分隔
				rec.setColType(3, ValueType.STRING);//客户端版本
				rec.setColType(4, ValueType.STRING);
				rec.setColType(5, ValueType.LONG);
				rec.setColType(6, ValueType.LONG);
			}
			int row = rec.findRow(0,0,backMaintain.getId());
			if (row != -1) {
				rec.removeRow(row);
			}
			List<Integer> placeIdList = backMaintain.getPlaceIdList();
			StringBuffer placeIds = new StringBuffer();
			for (int placeId : placeIdList) {
				placeIds.append(placeId + ",");
			}
			placeIds.deleteCharAt(placeIds.length() - 1);
			String version = backMaintain.getVersion();
			String message = backMaintain.getMessage();
			long start     = backMaintain.getStart();
			long end       = backMaintain.getEnd();
			rec.addRow(id,operation,placeIds.toString(),version,message,start,end);
		} else {//删除
			if (rec != null) {
				int row = rec.findRow(0, 0, id);
				if (row != -1) {
					logger.info("del pub maintain id:{}", id);
					rec.removeRow(row);
				}
			}
		}
		kernel.storePubData(pubData);
	}

	public void OnBackFeedbackService(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.StringSingle feedbackService = ServerMsg.StringSingle.parseFrom(data);
		String content = feedbackService.getWords();
		IPubData pubData = kernel.getPubData("PubFeedbackData",true);
		if (pubData.getValue("FeedBackService") == null) {
			pubData.addProperty("FeedBackService", ValueType.STRING, content, true);
		} else {
			pubData.setValue("FeedBackService", content);
		}
		kernel.storePubData(pubData);
	}

	public void OnBackPubNotice(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.BackPubNotice backPubNotice = ServerMsg.BackPubNotice.parseFrom(data);
		int type = backPubNotice.getType();
		if (type == NoticeType.LOGIN.ordinal()) {
			IPubData pubData = kernel.getPubData("PubNoticeData",true);
			IPubRecord rec = pubData.getRecord("PubNoticeTable");
			if (rec == null) {
				rec = pubData.addRecord("PubNoticeTable", 8, 10, true);
				rec.setColType(0, ValueType.INT);
				rec.setColType(1, ValueType.INT);
				rec.setColType(2, ValueType.STRING);
				rec.setColType(3, ValueType.STRING);
				rec.setColType(4, ValueType.STRING);
				rec.setColType(5, ValueType.STRING);
				rec.setColType(6, ValueType.STRING);
				rec.setColType(7, ValueType.BOOL);
			}
			int row = rec.findRow(0, 0, backPubNotice.getId());
			if (row == -1) {
				int id = backPubNotice.getId();
				List<Integer> placeIdList = backPubNotice.getPlaceIdList();
				StringBuffer placeIds = new StringBuffer();
				for (int placeId : placeIdList) {
					placeIds.append(placeId + ",");
				}
				placeIds.deleteCharAt(placeIds.length() - 1);
				String tag = backPubNotice.getTag();
				String title = backPubNotice.getTitle();
				String content = backPubNotice.getContent();
				String picture = backPubNotice.getPicture();
				boolean mail = backPubNotice.getMail();
				rec.addRow(id, type, placeIds.toString(), tag, title, content, picture, mail);
			} else {
				rec.setValue(row, 3, backPubNotice.getTag());
				rec.setValue(row, 4, backPubNotice.getTitle());
				rec.setValue(row, 5, backPubNotice.getContent());
				rec.setValue(row, 6, backPubNotice.getPicture());
				rec.setValue(row, 7, backPubNotice.getMail());
			}
			kernel.storePubData(pubData);
		}
	}

	public void OnBackRepNotice(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.BackPubNotice backPubNotice = ServerMsg.BackPubNotice.parseFrom(data);
		int id = backPubNotice.getId();
		int type = backPubNotice.getType();
		if (type == NoticeType.LOGIN.ordinal()) {
			IPubData pubData = kernel.getPubData("PubNoticeData",true);
			IPubRecord rec  = pubData.getRecord("PubNoticeTable");
			if (rec != null) {
				int row = rec.findRow(0, 0, id);
				if (row != -1) {
					logger.info("del pub notice id:{}", id);
					rec.removeRow(row);
					kernel.storePubData(pubData);
				}
			}
		}
	}

	public void OnRecvAllRoomRunInfo(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		logger.info("pub OnRecvRoomInfo");
		ServerMsg.StringArray gameServers = ServerMsg.StringArray.parseFrom(data);
		List<String> gameSvrList = gameServers.getSomewordsList();
		IPubData pubRoomData = kernel.getPubData("RoomData",false);
		IPubRecord rec = pubRoomData.getRecord("PlayerCount");
		ServerMsg.RoomDatas.Builder build = ServerMsg.RoomDatas.newBuilder();
		int[] roomIds = { 0, 1, 2, 3 }; //去除鱼王争霸
//		int[] roomIds = { 0, 1, 2, 3, 8 };
		for (int roomid : roomIds) {
			logger.info("pub : roomid : {}", roomid);
			int row = -1;
			if (rec != null) {
				row = rec.findRow(0, 0, roomid);
			}
			int online = 0;
			if (row != -1) {
				online = rec.getInt(row, 1);
			}
			long totalPlay = 0;
			long totalWin = 0;
			for (String server : gameSvrList) {
				IPubData pubPWData = kernel.getPubData(server + "_room_" + "room" + roomid,false);
				if (pubPWData != null) {
					totalPlay += pubPWData.getLong(PLAYER_PROPERTY_TOTALPLAY);
					totalWin  += pubPWData.getLong(PLAYER_PROPERTY_TOTALWIN);
					logger.info("pub: totalPlay: {} tatalWin: {}", totalPlay, totalWin);
				}
			}
			ServerMsg.RoomData.Builder info = ServerMsg.RoomData.newBuilder();
			info.setId(roomid);
			info.setTotalPlay(totalPlay);
			info.setTotalWin(totalWin);
			info.setTotalGet(totalPlay - totalWin);
			info.setOnline(online);
			build.addRoomDatas(info.build());
		}
		//发送消息到game获取房间其他参数
		for (String server : gameSvrList) {
			kernel.sendServerMsg(server,ServerMsgDef.P2G_ALLROOM_RUNINFO.ordinal(), build.build().toByteArray());
		}
	}


	void OnPubdataLoad(IPubKernel kernel, String none) {
		IPubData pubData = kernel.getPubData("RoomData",true);
		IPubRecord rec = pubData.getRecord("PlayerCount");
		if (rec == null){
			//增加虚拟人数数据和虚拟人数数据刷新时间
			rec   = pubData.addRecord("PlayerCount",4, 20, true);
			rec.setColType(PLAYER_COUNT_COL.COL_ROOM_ID.ordinal(), ValueType.INT);
			rec.setColType(PLAYER_COUNT_COL.COL_PLAYER_COUNT.ordinal(), ValueType.INT);
			rec.setColType(PLAYER_COUNT_COL.COL_VIRTUAL_COUNT.ordinal(), ValueType.INT);
			rec.setColType(PLAYER_COUNT_COL.COL_REFRESH_TIME.ordinal(), ValueType.LONG);
		}else{
			rec.clear();//清理上次游戏缓存
		}
		kernel.storePubData(pubData);
		pubData = kernel.getPubData("PubMaintainData",false);
		if (pubData == null){
			return;
		}
		rec = pubData.getRecord("PubMaintainTable");
		if (rec == null){
			return;
		}
		long now = System.currentTimeMillis();
		boolean needSave = false;
		for (int i = 0; i < rec.getRows() ;) {
			long end = rec.getLong(i,6);//结束时间
			if (now > end){
				rec.removeRow(i);
				needSave = true;
			}else{
				i++;
			}
		}
		if (needSave){
			kernel.storePubData(pubData);
		}
	}

	public void OnCheckGoldEggVer(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.Int64 msg = ServerMsg.Int64.parseFrom(data);
		long ver = msg.getValue();
		IPubData pubData = kernel.getPubData("HitEggAwardData",true);
		if (pubData.getValue("HitEggAwardVersion") == null) {
			pubData.addProperty("HitEggAwardVersion", ValueType.LONG, ver, true);
			if (pubData.getValue("HitEggAwardValue") == null) {
				pubData.addProperty("HitEggAwardValue", ValueType.LONG, 10000000L, true);
			} else {
				pubData.setValue("HitEggAwardValue", 10000000L);
			}
		} else if ((long) pubData.getValue("HitEggAwardVersion") != ver) {
			pubData.setValue("HitEggAwardVersion", ver);
			pubData.setValue("HitEggAwardValue", 10000000L);
			//版本不一致，清榜单
			IPubRecord rec = pubData.getRecord("HitEggAwardShowRec");
			if (rec != null) {
				rec.clear();
			}
		}
		kernel.storePubData(pubData);
	}

	public void OnHitEggAwardChange(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.HitEggAwardValue awardValue = ServerMsg.HitEggAwardValue.parseFrom(data);
		IPubData pubData = kernel.getPubData("HitEggAwardData",true);
		if (pubData.getValue("HitEggAwardValue") == null) {
			pubData.addProperty("HitEggAwardValue", ValueType.LONG, awardValue.getValue() + 10000000L, true);
		} else {
			pubData.setValue("HitEggAwardValue", (long) pubData.getValue("HitEggAwardValue") + awardValue.getValue());
		}
		kernel.storePubData(pubData);
	}

	public void OnClearHitEggAwardValue(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		logger.info("invoke OnClearHitEggAwardValue");
		IPubData pubData = kernel.getPubData("HitEggAwardData",false);
		if (pubData != null) {
			if (pubData.getValue("HitEggAwardValue") != null) {
				pubData.setValue("HitEggAwardValue", 10000000L);
				kernel.storePubData(pubData);
			}
		}
	}

	public void OnAddHitEggAwardShow(IPubKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
		ServerMsg.HitEggAwardInfo awardInfo = ServerMsg.HitEggAwardInfo.parseFrom(data);
		IPubData pubData = kernel.getPubData("HitEggAwardData",true);
		IPubRecord rec = pubData.getRecord("HitEggAwardShowRec");
		if (rec == null) {
			rec = pubData.addRecord("HitEggAwardShowRec", 3, 10, true);
			rec.setColType(0, ValueType.STRING);// 玩家名
			rec.setColType(1, ValueType.STRING);// itemId
			rec.setColType(2, ValueType.INT);// itemCount
		}
		if (rec.getRows() >= 10) {
			rec.removeRow(0);
		}
		rec.addRow(awardInfo.getPlayerName(), awardInfo.getItemId(), awardInfo.getItemCount());
		kernel.storePubData(pubData);
	}

	public void OnUpdatePublicData(IPubKernel kernel, int msgid, byte[] data) throws InvalidProtocolBufferException {
		logger.info("OnUpdatePublicData,msgid:{}", msgid);
		ServerMsg.UpdatePubData updatePubData = ServerMsg.UpdatePubData.parseFrom(data);
		ServerMsg.UpdatePubDataRes.Builder build = ServerMsg.UpdatePubDataRes.newBuilder();
		String pubName = updatePubData.getDataName();
		IPubData pubData = kernel.getPubData(pubName,false);
		String tableName = updatePubData.getTableName();
		String res = "";
		int opt = updatePubData.getOpt();
		if (GMModule.PublicOpt.GET_PRO.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				Object objValue = pubData.getValue(updatePubData.getProName());
				if (objValue == null) {
					res = "属性值不存在";
				} else {
					res = objValue.toString();
				}
			}
		} else if (GMModule.PublicOpt.SET_PRO.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				ValueType valueType = pubData.getType(updatePubData.getProName());
				Object objValue = UtilFunc.getValueByType(updatePubData.getProValue(), valueType);
				pubData.setValue(updatePubData.getProName(), objValue);
				res = updatePubData.getProName() + " =" + pubData.getValue(updatePubData.getProName());
			}
		} else if (GMModule.PublicOpt.CLEAR_REC.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				IPubRecord rec = pubData.getRecord(tableName);
				if (rec != null) {
					rec.clear();
					res = "clear success. left " + rec.getRows();
				} else {
					res = "记录不存在";
				}
			}
		} else if (GMModule.PublicOpt.DEL_ROW.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				IPubRecord rec = pubData.getRecord(tableName);
				rec.removeRow(updatePubData.getRow());
				res = "del row success. left " + rec.getRows();
			}
		} else if (GMModule.PublicOpt.ADD_ROW.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				IPubRecord rec = pubData.getRecord(tableName);
				String[] inputs = updatePubData.getValue().split(",");
				int cols = rec.getCols();
				List<Object> args = new LinkedList<>();
				if (inputs.length != cols) {
					res = "参数不正确";
				} else {
					for (int i = 0; i < cols; i++) {
						Object obj = UtilFunc.getValueByType(inputs[i], rec.getColType(i));
						args.add(obj);
					}
					if (rec.addRow(args.toArray())) {
						res = "success";
					} else {
						res = "fail";
					}
				}
			}
		} else if (GMModule.PublicOpt.GET_TABLE.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				IPubRecord rec = pubData.getRecord(tableName);
				if (rec == null) {
					res = "record [" + tableName + "] not exist";
				} else {
					int rows = rec.getRows();
					int cols = rec.getCols();
					res = "record [" + tableName + "] = (" + rows + "X" + cols + ")\n";
					for (int i = 0; i < rows; ++i) {
						String rowVal = "r" + i + " = ";
						for (int j = 0; j < cols; ++j) {
							rowVal += rec.getValue(i, j).toString() + ", ";
						}
						res += rowVal + "\n";
					}
				}
			}
		} else if (GMModule.PublicOpt.SET_TABLE.ordinal() == opt) {
			if (pubData == null){
				res = "找不到" + pubName;
			}else{
				int row = updatePubData.getRow();
				int col = updatePubData.getCol();
				Object val = null;
				IPubRecord rec = pubData.getRecord(tableName);
				ValueType type = rec.getColType(updatePubData.getCol());
				val = UtilFunc.getValueByType(updatePubData.getCellValue(), type);
				if (val != null) {
					rec.setValue(updatePubData.getRow(), updatePubData.getCol(), val);
					res = "set " + tableName + "(" + row + ", " + col + ") = " + rec.getValue(row, col);
				}
			}
		} else if (GMModule.PublicOpt.CREATE_PUB.ordinal() == opt) {
			pubData = new PubData(pubName);
			if (kernel.storePubData(pubData)) {
				res = "success";
			} else {
				res = "fail";
			}
		} else if (GMModule.PublicOpt.DEL_PUB.ordinal() == opt) {
			if (kernel.deletePubData(pubName)) {
				res = "success";
			} else {
				res = "fail";
			}
		}
		build.setRes(res);
		kernel.responseServer(msgid, build.build().toByteArray());
	}
}
