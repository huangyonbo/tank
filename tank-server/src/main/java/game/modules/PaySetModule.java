/**   
*    
* 描述：邮件模块   
* 文件：MailModule.java
* 创建人：胡中伟
* 创建时间：2018年4月13日 上午11:06:44 
*    
*/
package game.modules;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsgDef;
import framework.JsonUtil;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.mybatis.domain.PaySet;
import framework.mybatis.service.impl.PaySetService;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.modules.utils.UtilFunc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 		
 * 描述：
 * 
 */
public class PaySetModule implements ILogicModule {
	
	Map<Integer, Map<String, Integer>> m_mapPaySet = new HashMap<>();
	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regServerRequest(ServerMsgDef.B2G_UPDATE_PAYSET.ordinal(), this, "OnUpdatePaySet");
		kernel.regRequestMessage(RequestMsgDef.REQ_GET_PAY_WAYS.ordinal(), this, "OnGetPayWays");
		return true;
	}

	@Override
	public void onDestroy() {

	}
	
	@Override
	public void onNetReady(IKernel kernel) {
		// 加载支付设置
		kernel.executeSomeToStore(PaySetService.class, "loadAll", null, (str) -> {
			List<PaySet> paySet = framework.JsonUtil.decodeToList(str, PaySet.class);
			if (paySet != null) {
				for (PaySet payset : paySet) {
					m_mapPaySet.put(payset.getChannel(), JsonUtil.decodeToMap(payset.getPaySet(), String.class, Integer.class));
				}
			}
		});
	}
	 
	// 更新支付设置
	void OnUpdatePaySet(IKernel kernel, int msgid, byte[] data) 
			throws InvalidProtocolBufferException {
		CustomMsg.String msg = CustomMsg.String.parseFrom(data);
		String jsonStr = msg.getValue();
		JsonObject json = JsonUtil.decodeToObj(jsonStr, JsonObject.class);
		int channel = json.get("channel").getAsInt();
		m_mapPaySet.put(channel, JsonUtil.decodeToMap(json.get("payset").getAsString(), String.class, Integer.class));
		kernel.responseServer(msgid, new byte[0]);
	}
	
	// 客户端获取支付方式
	public void OnGetPayWays(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
			throws InvalidProtocolBufferException {
		JsonObject json = new JsonObject();
		int channel = player.getInt(PLAYER_PROPERTY_CHANNEL);
		if (m_mapPaySet.containsKey(channel)){
			json.add("payset", JsonUtil.encodeToElement(m_mapPaySet.get(channel)));
		} else {
			Map<String, Integer> tmp = new HashMap<>();
			tmp.put("wechat", 1);
			tmp.put("alipay", 1);
			tmp.put("lakalaWechat", 0);
			tmp.put("lakalaAlipay", 0);
			json.add("payset", JsonUtil.encodeToElement(tmp));
		}
		UtilFunc.respRpcStringToClient(kernel, player, reqid, json.toString());
	}
}
