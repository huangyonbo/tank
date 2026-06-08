package game.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import game.constant.OfflineDataType;
import game.custommsg.C2SMsgDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;

// 聊天模块
public class ChatModule implements ILogicModule {

	private static Logger logger = LoggerFactory.getLogger(ChatModule.class);

	private OfflineDataModule offlineDataModule;

	@Override
	public void onDestroy() {

	}

	@Override
	public boolean onInit(IKernel kernel) {

		kernel.regClientMessage(C2SMsgDef.C2S_CHAT.ordinal(), this, "OnChat");
		// 注册server消息
		kernel.regServerMsg(ServerMsgDef.MMSG_CHAT_SET.ordinal(), this, "OnChatSet");
		offlineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");
		return true;
	}

	public void OnChat(IKernel kernel, IGameObject player, int msgid, byte[] msg)
			throws InvalidProtocolBufferException {

		int nUid = player.getInt(PLAYER_PROPERTY_UID);

		long lDeskId = player.getLong(PLAYER_PROPERTY_DESKID);
		IGameObject desk = kernel.getGameObject(lDeskId);

		Short shSeatId = player.getShort(PLAYER_PROPERTY_SEATID);
		if (null == desk) {
			logger.error("uid:{} not on seat, can not chat.", nUid);
			return;
		}

		CustomMsg.Chat chat = CustomMsg.Chat.parseFrom(msg);
		int nChatType = chat.getChatType();
		if (nChatType < 1) {
			logger.error("uid:{} send illegal chat type.", nUid);
			return;
		}
		// 直接转发，具体类型合法性由客户端检验

		CustomMsg.SyncChat.Builder syncChat = CustomMsg.SyncChat.newBuilder();
		syncChat.setChatType(nChatType);
		syncChat.setSeatId(shSeatId.intValue());

		kernel.broadCastByKen(player, S2CMsgDef.S2C_SYCN_CHAT.ordinal(), syncChat.build().toByteArray());

	}

	public void OnChatSet(IKernel kernel, int serid, int msgid, byte[] msg) throws InvalidProtocolBufferException {
		ServerMsg.ChatSet chatSet = ServerMsg.ChatSet.parseFrom(msg);
		long time = chatSet.getTime();
		int playerId = chatSet.getPlayerId();
		IGameObject player = kernel.getPlayer(playerId);
		if (null != player) {
			player.setProperty(PLAYER_PROPERTY_SHUTUP, System.currentTimeMillis() + time);
		} else {
			offlineDataModule.AddOfflineData(kernel, playerId, OfflineDataType.SHUT_UP,
					"" + (System.currentTimeMillis() + time), PLAYER_PROPERTY_SHUTUP);
		}
	}
}
