package back.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import back.modules.data.ItemData;
import back.modules.data.Write;
import back.modules.data.manualpay.Pay;
import back.modules.data.paymanage.ResupplyPay;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;
import game.custommsg.CustomMsg;

/**
 * 
 * 描述：支付补单
 * 
 */
public class PayModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(PayModule.class);

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

	public void resupplyPay(BacKernel kernel, ResupplyPay resupplyPay, IDataCallBack cb) {
		int uid = resupplyPay.getUid();
		ItemData[] itemDatas = resupplyPay.getData();

		try {
			ServerMsg.SysMail.Builder build = ServerMsg.SysMail.newBuilder();
			int id = 0;//未用
			String title = "补单邮件";
			String content = "您的补单到账了，请领取";

			int expiry = -1;

			build.setId(id);
			build.addReceivers(uid);
			build.setTitle(title);
			build.setContent(content);
			for (ItemData data : itemDatas) {
				ServerMsg.ItemData.Builder info = ServerMsg.ItemData.newBuilder();
				info.setItemId(data.getId());
				info.setCount(data.getCount());
				build.addItemDatas(info.build());
			}
			build.setExpiry(expiry);

			// 驱动游戏服按邮件地址发邮件
			logger.info("send to game to send sys mail ...");
			Object[] gameServers = kernel.getServersByType("game");
			kernel.sendServerMsg((String) gameServers[0], ServerMsgDef.B2G_SYSMAIL.ordinal(),
					build.build().toByteArray());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 后台页面要一个返回来终止连接
			cb.push(new Write());
		}
	}
	public void fireManualPay(BacKernel kernel, Pay pay, IDataCallBack cb) {
		int uid = pay.getUid();
		ItemData[] itemDatas = pay.getData();
		
		try {
			ServerMsg.SysMail.Builder build = ServerMsg.SysMail.newBuilder();
			int id = 0;//未用
			String title = "人工充值";
			String content = "您的充值到账了，请领取";

			int expiry = -1;

			build.setId(id);
			build.addReceivers(uid);
			build.setTitle(title);
			build.setContent(content);
			for (ItemData data : itemDatas) {
				ServerMsg.ItemData.Builder info = ServerMsg.ItemData.newBuilder();
				info.setItemId(data.getId());
				info.setCount(data.getCount());
				build.addItemDatas(info.build());
			}
			build.setExpiry(expiry);

			// 驱动游戏服按邮件地址发邮件
			logger.info("send to game to send sys mail ...");
			Object[] gameServers = kernel.getServersByType("game");
			kernel.sendServerMsg((String) gameServers[0], ServerMsgDef.B2G_PAYMAIL.ordinal(),
					build.build().toByteArray());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 后台页面要一个返回来终止连接
			cb.push(new Write());
		}
	}
	
	// 更新支付方式开关
	public void updatePaySet(BacKernel kernel, int channel, String payset, IDataCallBack cb) {
		JsonObject json = new JsonObject();
		json.addProperty("channel", channel);
		json.addProperty("payset", payset);
		CustomMsg.String.Builder builder = CustomMsg.String.newBuilder();
		builder.setValue(json.toString());
		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_GAME,ServerMsgDef.B2G_UPDATE_PAYSET.ordinal(),builder.build().toByteArray(),
				res -> cb.push(res.length == 0 ? new Write() : new Write("更新失败"))
		);
	}
}
