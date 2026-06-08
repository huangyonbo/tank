package back.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.Write;
import back.modules.data.propmanage.UpdateConfig;
import back.modules.data.propmanage.UpdateItem;
import back.modules.dataenum.Code;
import common.ServerMsg;
import common.ServerMsgDef;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：道具管理
 * 
 */
public class PropModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(PropModule.class);

	private int recvCount = 0;
	private boolean success = true;

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

	public void updateConfig(BacKernel kernel, UpdateConfig updateConfig, IDataCallBack cb) {

		String[] pathArr = updateConfig.getFilePath();
		Object[] sers = kernel.getServersByType("game");
		if(sers.length == 0)
		{
			cb.push(new Write(Code.FAIL));
			return;
		}

		ServerMsg.StringArray.Builder build = ServerMsg.StringArray.newBuilder();

		for (String path : pathArr) {
			build.addSomewords(path);
		}

		if(recvCount != 0)
		{
			cb.push(new Write(Code.FAIL));
			return;
		}

		recvCount++;
		success = true;
		for (int i = 0; i < sers.length; ++i) {
			String name = sers[i].toString();
			kernel.requestServer(name, ServerMsgDef.M2G_UPDATE_CFG.ordinal(), build.build().toByteArray(), (byte[] resData) -> {
				logger.info("{} : update cfg {}", name, (resData[0] == 1 ? "success." : "failed."));
				if (resData[0] != 1) {
					success = false;
				}
				recvCount++;
				if (recvCount == sers.length + 1) {
					if (success) {
						cb.push(new Write());
						return;
					} else {
						cb.push(new Write(Code.FAIL));
					}
					recvCount = 0;
				}
			});
		}
	}

	public void updateItem(BacKernel kernel, UpdateItem update, IDataCallBack cb) 
	{
		String item = update.getId();
		int stock = update.getStock();
		int limit = update.getLimit();
		boolean reset = update.isDayReset();
		
		ServerMsg.UpdateStoreExchange.Builder build = ServerMsg.UpdateStoreExchange.newBuilder();
		build.setItem(item);
		build.setStock(stock);
		build.setLimit(limit);
		build.setReset(reset);

		kernel.requestServer(framework.ServerSet.SERVER_LOGIC_NAME_PUBLIC, ServerMsgDef.B2P_UPDATE_EXCHANGE.ordinal(), build.build().toByteArray(), (byte[] resData) -> {
			cb.push(new Write());
		});
	}
}
