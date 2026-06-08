/**   
*    
* 描述：   加速技能
* 文件：SpeedSkill.java
* 创建人：胡中伟
* 创建时间：2018年4月10日 下午4:30:29 
*    
*/
package game.modules.skills;

import java.util.HashMap;
import java.util.Map;

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
public class CliShow extends BaseSkill {
	
	private Map<Integer, Integer> m_mapSkillLevelByVip = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> m_mapSkillTimeByLevel = new HashMap<Integer, Integer>();
	private int m_unlockVipLevel;
	
	@Override
	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		BaseSkillData baseSkillData = new BaseSkillData();
		baseSkillData.type =cfg.getInt(index, "Type");
		baseSkillData.skillid = cfg.getString(index, "Id");
		SetType(baseSkillData.type);
		
		m_unlockVipLevel = cfg.getInt(index, "LockVipLv");
		int[] skillLevelByVip = cfg.getIntArray(index, "VipLvToSkillLv", ";");
		for(int i = 0; i < skillLevelByVip.length; i++) {
			m_mapSkillLevelByVip.put(i, skillLevelByVip[i]);
		}
		
		int[] skillTimeByLevel = cfg.getIntArray(index, "Time", ";");
		for(int i = 0; i < 1; i++) {
			m_mapSkillTimeByLevel.put(i + 1, skillTimeByLevel[i]);
		}
		
		return baseSkillData;
	}
	
	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if(vipLevel < m_unlockVipLevel)
			return;
		int skillLv = 1;
		if(m_mapSkillLevelByVip.containsKey(vipLevel)) {
			skillLv = m_mapSkillLevelByVip.get(vipLevel);
		}
		
		// record tag
		CustomMsg.OnUseSkill.Builder build = CustomMsg.OnUseSkill.newBuilder();
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		build.setSkill(skillData.skillid);
		build.setLevel(skillLv);
		kernel.broadCastByKen(player, S2CMsgDef.S2C_ON_USE_SKILL.ordinal(), build.build().toByteArray());
        if (skillData.skillid.equals("skill_speed")) {
            player.setProperty(PLAYER_SKILL_SPEED_END_TIME, kernel.getServerTime() + 20000);
            if (player.getBool("IsRobot")) {
                player.setProperty("SpeedEndTime", kernel.getServerTime() + 20000);
            }
        }
	}
}
