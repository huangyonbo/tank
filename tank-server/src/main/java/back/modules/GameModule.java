package back.modules;

import back.modules.data.Write;
import back.modules.data.extremetable.Desk;
import back.modules.data.extremetable.DeskData;
import back.modules.data.room.CustomDeskPlayerDTO;
import back.modules.data.room.BaseRoomDTO;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ServerSet;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import game.custommsg.CustomMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * 描述：游戏设置
 * 
 */
public class GameModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(GameModule.class);

	private IDataCallBack cb = null;
//	private Room room = null;

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(BacKernel kernel) {
		kernel.regServerMsg(ServerMsgDef.G2B_ALLROOM_DATA.ordinal(), this, "OnRecvAllRoomData");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void getRoomData(BacKernel kernel, IDataCallBack cb) {
		// int gameType = room.getGameType();// 暂未用

		this.cb = cb;
//		this.room = room;

		// 请求game服room参数
		ServerMsg.StringArray.Builder build = ServerMsg.StringArray.newBuilder();
		logger.info("send to game to get ...");
		Object[] gameServers = kernel.getServersByType("game");
		for (Object gameSvr : gameServers) {
			build.addSomewords((String) gameSvr);
		}
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_ALLROOM_DATA.ordinal(), build.build().toByteArray());
	}

	public void OnRecvAllRoomData(BacKernel kernel, int serid, int msgid, byte[] data)
			throws InvalidProtocolBufferException {
//		List<RoomData> list = new ArrayList<RoomData>();

		// 解析game服返回的roomData数据
		ServerMsg.RoomDatas response = ServerMsg.RoomDatas.parseFrom(data);

		List<BaseRoomDTO> collect = response.getRoomDatasList().stream().map(e -> {
			BaseRoomDTO roomDTO = new BaseRoomDTO();
			roomDTO.setRoomId(e.getId());
			roomDTO.setOnlinePlayer(e.getOnline());
			roomDTO.setTotalPlay((long) e.getTotalPlay());
			roomDTO.setTotalWin((long) e.getTotalWin());
			roomDTO.setTotalDeviation(roomDTO.getTotalPlay() - roomDTO.getTotalWin());
			roomDTO.setMinBulletValue(e.getMinGun());
			roomDTO.setMaxBulletValue(e.getMaxGun());
			roomDTO.setVipLevelLimit(e.getVipLevel());
			roomDTO.setAutoKickTime(e.getAutoKick());
			return roomDTO;
		}).collect(Collectors.toList());
		this.cb.push(collect);
//
//		for (ServerMsg.RoomData roomDataPro : listRoomDataPro) {
//			RoomData roomData = new RoomData();
//			roomData.setId(roomDataPro.getId());
//			roomData.setAutoKick(roomDataPro.getAutoKick());
//			roomData.setMaxGun(roomDataPro.getMaxGun());
//			roomData.setMinGun(roomDataPro.getMinGun());
//			roomData.setVipLevel(roomDataPro.getVipLevel());
//			roomData.setOnline(roomDataPro.getOnline());
//			roomData.setTotalPlay(roomDataPro.getTotalPlay());
//			roomData.setTotalWin(roomDataPro.getTotalWin());
//			roomData.setTotalGet(roomDataPro.getTotalGet());
//			list.add(roomData);
//		}
//		logger.info("send back to Back server ...");
//		room.setRoot(list);
//		this.cb.push(room);
	}

	public void getExtremeDeskData(BacKernel kernel, Desk desk, IDataCallBack cb) {
		ServerMsg.CustomDesk.Builder customDesk = ServerMsg.CustomDesk.newBuilder();
		customDesk.setDeskId(desk.getDeskId());
		customDesk.setDeskType(desk.getDeskType());
		Object[] gameServers = kernel.getServersByType("game");
		kernel.requestServer((String) gameServers[0], ServerMsgDef.B2G_GET_CUSTOM_DESK_DATA.ordinal(), customDesk.build().toByteArray(), (data) -> {
			ServerMsg.CustomDeskData res;
			try {
				res = ServerMsg.CustomDeskData.parseFrom(data);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			List<DeskData> list = new ArrayList<>();
			for (ServerMsg.CustomSeatData customSeatData : res.getSeatDataList()) {
				DeskData deskData = new DeskData();
				deskData.setDeskId(customSeatData.getDeskId());
				deskData.setSeatId(customSeatData.getSeatId());
				deskData.setUid(customSeatData.getUid());
				deskData.setTotalPlay(customSeatData.getTotalPlay());
				deskData.setTotalWin(customSeatData.getTotalWin());
				deskData.setVipLevel(customSeatData.getVipLevel());
				deskData.setMojin(customSeatData.getMojin());
				deskData.setSanchaji1(customSeatData.getSanchaji1());
				deskData.setSanchaji2(customSeatData.getSanchaji2());
				list.add(deskData);
			}
			desk.setRoot(list);
			cb.push(desk);
		});
	}
	public void listCustomGamePlayer(BacKernel kernel, int deskId, int deskType, IDataCallBack cb) {
		ServerMsg.CustomDesk.Builder customDesk = ServerMsg.CustomDesk.newBuilder();
		customDesk.setDeskId(deskId);
		customDesk.setDeskType(deskType);
		kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_CUSTOM_DESK_DATA.ordinal(),
				customDesk.build().toByteArray(), data -> {
					ServerMsg.CustomDeskData res;
					try {
						res = ServerMsg.CustomDeskData.parseFrom(data);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					List<CustomDeskPlayerDTO> collect = res.getSeatDataList().stream().map(e -> {
						CustomDeskPlayerDTO deskPlayerDTO = new CustomDeskPlayerDTO();
						deskPlayerDTO.setDeskId(e.getDeskId());
						deskPlayerDTO.setSeatId(e.getSeatId());
						deskPlayerDTO.setUid(e.getUid());
						deskPlayerDTO.setTotalPlay(e.getTotalPlay());
						deskPlayerDTO.setTotalWin(e.getTotalWin());
						deskPlayerDTO.setBombCoin(e.getMojin());
						deskPlayerDTO.setSkillNBomb(new Integer(e.getSanchaji1()).longValue());
						deskPlayerDTO.setSkillHBomb(new Integer(e.getSanchaji2()).longValue());
						return deskPlayerDTO;
					}).collect(Collectors.toList());
					cb.push(collect);
				});
	}

	// 更新海皇神殿配置
	public void updateSeakingConfig(BacKernel kernel, int roomtype, String value, IDataCallBack cb) {
		JsonObject json = new JsonObject();
		json.addProperty("roomtype", roomtype);
		json.addProperty("value", value);
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(json.toString());
		Object[] gameServers = kernel.getServersByType("game");
		kernel.requestServer((String) gameServers[0], ServerMsgDef.B2G_UPDATE_SEA_KING_CONFIG.ordinal(), builder.build().toByteArray(), (data) -> {
			int code = data[0];
			logger.info("updateGuildInfo|roomtype:{} value:{} code:{}", roomtype, value, code);
			cb.push(code);
		});
	}

	public void updateMagicConfig(BacKernel kernel, int roomType, String config, IDataCallBack cb) {
		JsonObject json = new JsonObject();
		json.addProperty("roomType", roomType);
		json.addProperty("config", config);
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(json.toString());
		Object[] gameServers = kernel.getServersByType("game");
		kernel.requestServer((String) gameServers[0], ServerMsgDef.B2G_UPDATE_SEA_KING_CONFIG.ordinal(), builder.build().toByteArray(),
				res -> cb.push(res.length == 0 ? new Write() : new Write(new String(res))));
	}
}
