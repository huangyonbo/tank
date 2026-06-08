package game.modules.skills;

import java.util.Map;
import java.util.HashMap;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;


/*
 * 狂暴技能
 * */
public class Rage extends BaseSkill{
	
	private static Map<Integer, Integer> m_mapSkillLevelByVip = new HashMap<Integer, Integer>();
	private static Map<Integer, Integer> m_mapSkillTimeByLevel = new HashMap<Integer, Integer>();
	private static int m_unlockVipLevel;
	
	@Override
	public BaseSkillData LoadConfig(ICfgReader cfg, int index) {
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
	
	@Override
	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData){
		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
		if(vipLevel < m_unlockVipLevel)
			return;
		
		int skillLevel = 0;
		if(m_mapSkillLevelByVip.containsKey(vipLevel)) {
			skillLevel = m_mapSkillLevelByVip.get(vipLevel);
		}
		
		CustomMsg.OnUseSkill.Builder build = CustomMsg.OnUseSkill.newBuilder();
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		build.setSkill(skillData.skillid);
		build.setLevel(skillLevel);
		kernel.broadCastByKen(player, S2CMsgDef.S2C_ON_USE_SKILL.ordinal(), build.build().toByteArray());
	}
}
