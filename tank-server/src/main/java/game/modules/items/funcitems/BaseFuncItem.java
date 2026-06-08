/**   
*    
* 描述：   
* 文件：BaseFuncItem.java
* 创建人：胡中伟
* 创建时间：2018年4月16日 上午9:50:40 
*    
*/
package game.modules.items.funcitems;

import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;

/**
 * 
 * 描述：
 * 
 */
public class BaseFuncItem implements PropertyKey {
	public boolean OnInit(IKernel kernel) {
		return true;
	}

	public void OnItemClassCreate(IKernel kernel, String script) {

	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {

	}
}
