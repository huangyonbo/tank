package game.modules;

import common.ServerMsg;
import common.ServerMsgDef;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;

public class StatisticsModule implements ILogicModule {

	@Override
	public void onDestroy() {

	}

	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
		kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerLogin");
		kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerLogout");
		return true;
	}

	public void OnPlayerClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, PLAYER_PROPERTY_STATISTICSLASTLOGINTIME, ValueType.LONG, false, true, true);
	}

	public void OnPlayerLogin(IKernel kernel, IGameObject player) {
		kernel.addLoginCount(player);
		player.setProperty(PLAYER_PROPERTY_STATISTICSLASTLOGINTIME, kernel.getServerTime());

		ServerMsg.PlayerStatisticsInfo.Builder info = ServerMsg.PlayerStatisticsInfo.newBuilder();
		info.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_PLAYER_LOGIN.ordinal(), info.build().toByteArray());
	}

	public void OnPlayerLogout(IKernel kernel, IGameObject player) {
		long statisticsLastLoginTime = player.getLong(PLAYER_PROPERTY_STATISTICSLASTLOGINTIME);
		int onlineTime = (int) (kernel.getServerTime() - statisticsLastLoginTime);
		kernel.addOnlineTime(player, onlineTime);
		ServerMsg.PlayerStatisticsInfo.Builder info = ServerMsg.PlayerStatisticsInfo.newBuilder();
		info.setChannel(player.getInt(PLAYER_PROPERTY_CHANNEL));
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_PLAYER_LOGOUT.ordinal(), info.build().toByteArray());
	}

}
