/**   
*    
* 描述：   
* 文件：RobotModule.java
* 创建人：胡中伟
* 创建时间：2018年9月5日 下午7:27:33 
*    
*/
package back.modules;

import back.modules.data.Write;
import back.modules.data.robotmanage.PlayRobotParam;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**   
*    
* 描述：   
*    
*/
public class PlayRobotModule implements IBackModule {

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

	public void useRobot(BacKernel kernel, PlayRobotParam param, IDataCallBack cb) {
		int uid = param.getUid();
		
		ServerMsg.UpdateRobot.Builder build = ServerMsg.UpdateRobot.newBuilder();
		build.setUid(uid);
		
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.B2M_UPDATE_ROBOT.ordinal(), build.build().toByteArray());
		
		cb.push(new Write());
	}

	public void unUseRobot(BacKernel kernel, PlayRobotParam param, IDataCallBack cb) {
		int uid = param.getUid();
		
		ServerMsg.UpdateRobot.Builder build = ServerMsg.UpdateRobot.newBuilder();
		build.setUid(uid);
		
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.B2M_UPDATE_ROBOT.ordinal(), build.build().toByteArray());
		
		cb.push(new Write());
	}

	public void updateRobot(BacKernel kernel, PlayRobotParam param, IDataCallBack cb) {
		int uid = param.getUid();
		
		ServerMsg.UpdateRobot.Builder build = ServerMsg.UpdateRobot.newBuilder();
		build.setUid(uid);
		
		kernel.sendServerMsg(framework.ServerSet.SERVER_LOGIC_NAME_MATCH, ServerMsgDef.B2M_UPDATE_ROBOT.ordinal(), build.build().toByteArray());
		
		cb.push(new Write());
	}
}
