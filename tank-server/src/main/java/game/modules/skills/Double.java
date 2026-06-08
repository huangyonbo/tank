package game.modules.skills;

import framework.PropertyKey;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;

class DoubleData extends BaseSkillData {
    int LockVipLv;
	String[] VipLvToSkillLv;
	String[] Multiple;
}

public class Double extends BaseSkill{
	
	public boolean OnInit(IKernel kernel) {
		kernel.declareHeartBeat("HB_CheckDoubleEnd", this, "OnDoubleEnd");
		return true;
	}
	
	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		DoubleData data = new DoubleData();
		data.type = m_type;
		data.LockVipLv = cfg.getInt(index, "LockVipLv");
		data.time = cfg.getStringArray(index, "Time",";");
		data.VipLvToSkillLv = cfg.getStringArray(index, "VipLvToSkillLv",";");
		data.Multiple = cfg.getStringArray(index, "Multiple",";");
		return data;
	}

	@Override
	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
//		int vipLevel = player.getInt(PLAYER_PROPERTY_VIPLEVEL);
//		DoubleData data = (DoubleData) skillData;
//        if (vipLevel < data.LockVipLv) return;
//		int SkillLv = Integer.parseInt(data.VipLvToSkillLv[vipLevel]);
//		int time = Integer.parseInt(data.time[SkillLv - 1]);
//		if (kernel.haveHeartBeat(player,"HB_CheckDoubleEnd")){
//			kernel.removeHeartBeat(player,"HB_CheckDoubleEnd");
//		}
//		int mult = Integer.parseInt(data.Multiple[SkillLv - 1]);
//		if (mult <= 0) {
//			mult = 1;
//		}
//		// 炮值不在使用技能的状态
//		if (player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE) <= 1) {
//			player.setProperty(PLAYER_PROPERTY_SKILLMULTIPLE, mult);
//			int myBulletValue = player.getInt(PLAYER_PROPERTY_BULLETVALUE);
//			player.setProperty(PLAYER_PROPERTY_BULLETVALUE, myBulletValue * mult); // 使用技能炮值翻倍
//		}
//
//		kernel.addHeartBeat("HB_CheckDoubleEnd", player, time, 1);
//
//		CustomMsg.OnUseSkill.Builder build = CustomMsg.OnUseSkill.newBuilder();
//		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
//		build.setSkill(skillData.skillid);
//		build.setLevel(SkillLv);
//		kernel.broadCastByKen(player, S2CMsgDef.S2C_ON_USE_SKILL.ordinal(), build.build().toByteArray());
	}
	
	void OnDoubleEnd(IKernel kernel, IGameObject player){
		// 技能取消  炮倍减半
		int mult = player.getInt(PLAYER_PROPERTY_SKILLMULTIPLE);
		if (mult <= 0) {
			mult = 1;
		}
		player.setProperty(PLAYER_PROPERTY_BULLETVALUE, player.getInt(PLAYER_PROPERTY_BULLETVALUE) / mult);
		player.setProperty(PLAYER_PROPERTY_SKILLMULTIPLE, 1);
	}
}
