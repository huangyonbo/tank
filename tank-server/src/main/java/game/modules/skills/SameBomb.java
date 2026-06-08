/**   
*    
* 描述：   同类炸弹
* 文件：SameBomb.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 下午4:32:29 
*    
*/
package game.modules.skills;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;

class SameBombData extends BaseSkillData {
	String fishtype;
}

/**
 * 
 * 描述：
 * 
 */
public class SameBomb extends BaseSkill {
	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		SameBombData data = new SameBombData();
		data.type = m_type;
		data.fishtype = cfg.getString(index, "FishID");

		return data;
	}

	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
		// record tag
	}
}
