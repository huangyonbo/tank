/**   
*    
* 描述：   
* 文件：SkillItem.java
* 创建人：胡中伟
* 创建时间：2018年4月18日 上午11:13:11 
*    
*/
package game.modules.items;

import game.custommsg.CommandDef;
import game.modules.player.TitleModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.*;

/**
 * 
 * 描述：
 * 
 */
public class TitleItem implements ILogicModule {
	private TitleModule m_titleModule = null;
	private static Logger logger = LoggerFactory.getLogger(TitleItem.class);

	public TitleItem(IKernel kernel) {
		kernel.addClass("TitleItem", "Item"); // 属性道具
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "TitleItem", this, "OnTitleClassCreate");
		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "TitleItem", this, "OnUseItem");

		kernel.preLoadConfig("res/Items/TitleItem.xml");

		m_titleModule = (TitleModule) kernel.getModule("TitleModule");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void OnTitleClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Title", ValueType.STRING, false, false, false);
	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];
		String title = item.getString("Title");
		int lifeTime = item.getInt("LifeTime");
		//logger.info("OnUse TitleItem {} {}", title, count);
		for (int i = 0; i < count; ++i) {
			m_titleModule.AddNewTitle(kernel, player, title, lifeTime);
		}
	}
}
