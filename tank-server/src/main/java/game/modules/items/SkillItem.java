/**   
*    
* 描述：   
* 文件：SkillItem.java
* 创建人：胡中伟
* 创建时间：2018年4月18日 上午11:13:11 
*    
*/
package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import common.ServerMsg;
import framework.net.message.InnerMsg;
import game.custommsg.*;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.modules.skills.SkillModule;

/**
 * 
 * 描述：
 * 
 */
public class SkillItem implements ILogicModule {
	private SkillModule m_SkillModule = null;
	static Logger logger = LoggerFactory.getLogger(SkillItem.class);

	public SkillItem(IKernel kernel) {
		kernel.addClass("SkillItem", "Item"); // 属性道具
	}

	/**
	 * @param kernel
	 * @return
	 */
	@Override
	public boolean onInit(IKernel kernel) {
		kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "SkillItem", this, "OnItemClassCreate");

		kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "SkillItem", this, "OnUseItem");

		kernel.preLoadConfig("res/Items/SkillItem.xml");

		m_SkillModule = (SkillModule) kernel.getModule("SkillModule");
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void onDestroy() {
	}

	public void OnItemClassCreate(IKernel kernel, String script) {
		kernel.declareProperty(script, "Skill", ValueType.STRING, false, false, false);
	}

	public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {

		IGameObject player = (IGameObject) objects[0];
		int count = (int) objects[1];

		String skill = item.getString("Skill");

		// logger.info("OnUse SkillItem {} {}", skill, count);

		for (int i = 0; i < count; ++i) {
			m_SkillModule.UseSkill(kernel, player, player, skill);
		}

		player.setProperty(PLAYER_PROPERTY_LASTHIT, kernel.getServerTime());
	}

}
