package framework.match;

public interface IMatchModule {
	/**
	 * 业务模块初始化
	 * 
	 * @param kernel
	 *            内核对象
	 * @return 是否成功
	 */
	boolean onInit(MatchKernel kernel);
	
	/**
	 * 当集群都启动成功后
	 * @param kernel
	 */
	default void onNetReady(MatchKernel kernel){
		
	}
	/**
	 * 业务模块销毁
	 * 
	 */
	void onDestroy();
}
