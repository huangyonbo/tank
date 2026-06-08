package framework;

import framework.net.ColonyType;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;

/**
 * 集群的心跳
 * @author ty
 *
 */
public class ColonyModuleHeart {
	String name;
	private long sendTime = 0;
	private long waitTime = 0;
	boolean stop = false;
	private static int SEND_TIME = 3000;//超时时间
	private static int OUT_TIME  = 6000;//超时时间
	
	public ColonyModuleHeart(String target) {
		name = target;
	}

	void reset(){
		sendTime = 0;
		waitTime = 0;
		stop = false;
	}
	
	boolean tick(BaseServer server,long now){
		if (stop){
			return false;
		}
		if (sendTime == 0){
			sendTime = now;
			return false;
		}
		if (now > sendTime + SEND_TIME){
			send(server);
			sendTime = now;
			if (waitTime == 0){
				waitTime = now;
			}
		}
		if (waitTime > 0 && now > waitTime + OUT_TIME){
			return true;
		}
		return false;
	}
	
	void resp(){
		waitTime = 0;
	}
	
	void send(BaseServer server){
		InnerMsg.ServerModuleData.Builder builder = InnerMsg.ServerModuleData.newBuilder();
		builder.setType(ColonyType.COLONY_TYPE_HEART.ordinal());
		builder.setCome(server.getName());
		builder.setDatas(server.getName() + "," + System.currentTimeMillis());
		byte[] datas = builder.build().toByteArray();
		server.sendMsgToServer(name,InnerMsgDef.INNER_MSG_REQ_SERVER_MODULE.ordinal(),datas);
	}
}
