/**   
*    
* 描述：   全屏炸弹
* 文件：BigBomb.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 下午4:31:55 
*    
*/
package game.modules.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;

/**
 * 
 * 描述：
 * 
 */
public class BigBomb extends BaseSkill {
	private static Logger logger = LoggerFactory.getLogger(BigBomb.class);

	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		BaseSkillData data = new BaseSkillData();
		data.type = m_type;

		return data;
	}

	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
		// record tag
		CustomMsg.OnUseSkill.Builder build = CustomMsg.OnUseSkill.newBuilder();
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		build.setSkill(skillData.skillid);
		kernel.broadCastByKen(player, S2CMsgDef.S2C_ON_USE_SKILL.ordinal(), build.build().toByteArray());

		logger.info("BigBomb OnUse {} {} {}", player.getString(PLAYER_PROPERTY_NAME), skillData.skillid, player.getShort(PLAYER_PROPERTY_SEATID));

	}
}
