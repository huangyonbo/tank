package framework.game;

import framework.PropertyKey;

/**
 * 
 * 描述： 逻辑对象接口
 * 
 */
public interface ILogicModule extends PropertyKey {
	/**
	 * 业务模块初始化
	 * 
	 * @param kernel
	 *            内核对象
	 * @return 是否成功
	 */
	boolean onInit(IKernel kernel);

	/**
	 * 当集群都启动成功后
	 * @param kernel
	 */
	default void onNetReady(IKernel kernel){
		
	}
	/**
	 * 业务模块销毁
	 * 
	 */
	default void onDestroy(){

	}
}
