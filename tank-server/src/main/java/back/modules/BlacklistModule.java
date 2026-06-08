package back.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import back.modules.data.Write;
import back.modules.data.blacklist.BlackOperate;
import back.modules.dataenum.Code;
import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

/**
 * 
 * 描述：黑名单管理
 * 
 */
public class BlacklistModule implements IBackModule {

	private static Logger logger = LoggerFactory.getLogger(BlacklistModule.class);

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

	public void freeze(BacKernel kernel, BlackOperate blackOperate, IDataCallBack cb) {
		int type = blackOperate.getType();
		String text = blackOperate.getText();
		logger.info("--------- blacklist freeze type : {}, text : {}", type, text);
		kernel.addBlackList(type, text, (Boolean data) -> {
			if(data) {
				cb.push(new Write());
			} else {
				cb.push(new Write(Code.FAIL));
			}
		});
	}

	public void unfreeze(BacKernel kernel, BlackOperate blackOperate, IDataCallBack cb) {
		int type = blackOperate.getType();
		String text = blackOperate.getText();
		logger.info("--------- blacklist unfreeze type : {}, text : {}", type, text);
		kernel.delBlackList(type, text, (Boolean data) -> {
			if(data) {
				cb.push(new Write());
			} else {
				cb.push(new Write(Code.FAIL));
			}
		});
	}

}
