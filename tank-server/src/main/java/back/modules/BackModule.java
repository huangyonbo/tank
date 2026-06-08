package back.modules;

import framework.back.BacKernel;
import framework.back.IBackModule;
import framework.back.net.IDataCallBack;

public class BackModule implements IBackModule {

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

	public void GetTestData(BacKernel kernel, int test1, String test2, IDataCallBack cb) {
		System.out.println("============================");
		cb.push(test1);
	}
}
