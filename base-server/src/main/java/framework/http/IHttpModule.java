package framework.http;

public interface IHttpModule
{
	/** 业务模块初始化
	 * @param kernel 内核对象
	 * @return 是否成功
	 */
	boolean onInit(HttpKernel kernel);
	
	/** 业务模块销毁
	 * 
	 */
	void onDestroy();
}
