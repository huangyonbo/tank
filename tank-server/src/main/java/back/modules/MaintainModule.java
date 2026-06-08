package back.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.Write;
import back.modules.data.maintainset.StartMaintain;
import back.modules.data.maintainset.StopMaintain;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：维护设置
 * 
 */
public class MaintainModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(MaintainModule.class);

	enum MaintainCmdDef {
		MAINTAIN_STOP, MAINTAIN_START,
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(BacKernel kernel) {
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void startMaintain(BacKernel kernel, StartMaintain sm, IDataCallBack cb) {
		try {
			ServerMsg.BackMaintainMsg.Builder build = ServerMsg.BackMaintainMsg.newBuilder();
			build.setId(sm.getId());
			build.setType(sm.getType());
			build.setOperation(MaintainCmdDef.MAINTAIN_START.ordinal());
			int[] placeIds = sm.getPlaceId();
			int count = placeIds.length;
			for (int i = 0; i < count; i++) {
				build.addPlaceId(placeIds[i]);
			}
			build.setVersion(sm.getVersion());
			build.setMessage(sm.getMessage());
			build.setStart(sm.getStart());
			build.setEnd(sm.getEnd());
			// 存储到pub服
			logger.info("send to pub to save ...");
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_MAINTAIN.ordinal(), build.build().toByteArray());
			logger.info("send to game(all) to mail & scroll & kick ...");
			kernel.broadToServer("game", ServerMsgDef.MMSG_MAINTAIN.ordinal(), build.build().toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 后台页面要一个返回来终止连接
			cb.push(new Write());
		}
	}

	public void stopMaintain(BacKernel kernel, StopMaintain sm, IDataCallBack cb) {
		try {
			ServerMsg.BackMaintainMsg.Builder build = ServerMsg.BackMaintainMsg.newBuilder();
			build.setId(sm.getId());
			build.setOperation(MaintainCmdDef.MAINTAIN_STOP.ordinal());
			//通知到pub服去删除
			logger.info("send to pub to del ...");
			kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.PUBMSG_MAINTAIN.ordinal(), build.build().toByteArray());
			//通知到game服去删除
			logger.info("send to game(all) to del ...");
			kernel.broadToServer("game", ServerMsgDef.MMSG_MAINTAIN.ordinal(),build.build().toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 后台页面要一个返回来终止连接
			cb.push(new Write());
		}
	}
}
