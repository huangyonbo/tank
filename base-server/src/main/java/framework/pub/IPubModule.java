/**   
*    
* 描述：   
* 文件：IPubModule.java
* 创建人：胡中伟
* 创建时间：2018年4月11日 下午2:11:11 
*    
*/
package framework.pub;

import framework.PropertyKey;

/**
 * 
 * 描述：
 * 
 */
public interface IPubModule extends PropertyKey {

	/**
	 * 业务模块初始化
	 * 
	 * @param kernel
	 *            内核对象
	 * @return 是否成功
	 */
	boolean onInit(IPubKernel kernel);

	/**
	 * 业务模块销毁
	 * 
	 */
	void onDestroy();
}
