package framework.back;

import framework.PropertyKey;

public interface IBackModule extends PropertyKey {
	/**
	 * 业务模块初始化
	 * 
	 * @param kernel
	 *            内核对象
	 * @return 是否成功
	 */
	default boolean onInit(BacKernel kernel){
		return true;
	};

	/**
	 * 业务模块销毁
	 * 
	 */
	default void onDestroy(){

	}
}
