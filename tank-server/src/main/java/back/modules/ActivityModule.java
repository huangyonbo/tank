package back.modules;

import back.modules.data.Write;
import back.modules.data.activitymanage.ActivityParam;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.ByteUtils;
import framework.ServerSet;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import game.custommsg.CustomMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityModule implements IBackModule {

	private static final Logger log = LoggerFactory.getLogger(ActivityModule.class);

	public void openActivity(BacKernel kernel, ActivityParam param, IDataCallBack cb) {
		Integer[] ids = param.getId();

		ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
		for (int id : ids) {
			build.addId(id);
		}

		kernel.broadToServer("game", ServerMsgDef.B2G_REFRESH_ACTIVITY.ordinal(), build.build().toByteArray());

		cb.push(new Write());
	}

	public void closeActivity(BacKernel kernel, ActivityParam param, IDataCallBack cb) {
		Integer[] ids = param.getId();

		ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
		for (int id : ids) {
			build.addId(id);
		}

		kernel.broadToServer("game", ServerMsgDef.B2G_CLOSE_ACTIVITY.ordinal(), build.build().toByteArray());

		cb.push(new Write());
	}
	public void UpdateThreeOneActivityWeight(BacKernel kernel, int param, IDataCallBack cb) {
		kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_THREE_SELECT_ONE_WEIGHT_CHANGE.ordinal(), null,res->{
            try {
                log.info("{}", ByteUtils.byteToObject(res).toString());
				Write data = new Write().SetMessage(ByteUtils.byteToObject(res).toString());
				cb.push(data);
            } catch (Exception e) {
				cb.push(new Write("fail"));
            }
        });
	}
	public void UpdateActivityWeight(BacKernel kernel, int type, IDataCallBack cb) {
		kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_ACTIVITY_UPDATE_CONFIG.ordinal(), CustomMsg.Int32.newBuilder().setValue(type).build().toByteArray(),res->{
			try {
				Write data = new Write().SetMessage(ByteUtils.byteToObject(res).toString());
				cb.push(data);
			} catch (Exception e) {
				cb.push(new Write("fail"));
			}
		});
	}

	public void delActivity(BacKernel kernel, ActivityParam param, IDataCallBack cb) {
		Integer[] ids = param.getId();
		ServerMsg.IntArray.Builder build = ServerMsg.IntArray.newBuilder();
		for (int id : ids) {
			build.addId(id);
		}
		kernel.broadToServer("game", ServerMsgDef.B2G_CLOSE_ACTIVITY.ordinal(), build.build().toByteArray());
		cb.push(new Write());
	}

	public void getNewPlayerLuckCardMallStock(BacKernel kernel, IDataCallBack callBack) {
		kernel.requestServer(ServerSet.SERVER_LOGIC_NAME_GAME, ServerMsgDef.B2G_GET_NEW_PLAYER_LUCKY_CARD_MALL_STOCK.ordinal(), new byte[0], callBack::push);
	}
}
