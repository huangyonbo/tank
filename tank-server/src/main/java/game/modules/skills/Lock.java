/**   
*    
* 描述：   
* 文件：Lock.java
* 创建人：胡中伟
* 创建时间：2018年5月3日 下午2:38:46 
*    
*/
package game.modules.skills;

import framework.PropertyKey;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import lombok.extern.slf4j.Slf4j;

class LockData extends BaseSkillData {
	String[] time;
	String[] VipLvToSkillLv;
}

/**
 * 
 * 描述：
 * 
 */
@Slf4j
public class Lock extends BaseSkill {

	public boolean OnInit(IKernel kernel) {
		kernel.declareHeartBeat("HB_CheckLockEnd", this, "OnLockEnd");
		kernel.regCommand(CommandDef.CMD_FUNC_FISH_SKILL_INIT.ordinal(), "Player", this, "CancleLock");
		return true;
	}

	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		LockData data = new LockData();
		data.type = m_type;
		data.time = cfg.getStringArray(index, "Time",";");
		data.VipLvToSkillLv = cfg.getStringArray(index, "VipLvToSkillLv",";");
		return data;
	}

	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
		LockData data = (LockData) skillData;
		int VipLv = (int) player.getProperty(PLAYER_PROPERTY_VIPLEVEL);
		int SkillLv = Integer.parseInt(data.VipLvToSkillLv[VipLv]);
		long now = kernel.getServerTime();
		long end = now + Integer.parseInt(data.time[SkillLv - 1]);
		player.setProperty(PLAYER_PROPERTY_LOCKEND, end);
		if (kernel.haveHeartBeat(player, "HB_CheckLockEnd")) {
			kernel.removeHeartBeat(player, "HB_CheckLockEnd");
		}
		kernel.addHeartBeat("HB_CheckLockEnd", player, Integer.parseInt(data.time[SkillLv - 1]), 1);
		CustomMsg.OnUseSkill.Builder build = CustomMsg.OnUseSkill.newBuilder();
		build.setSeatid(player.getShort(PLAYER_PROPERTY_SEATID));
		build.setSkill(skillData.skillid);
		build.setLevel(SkillLv);
		kernel.broadCastByKen(player, S2CMsgDef.S2C_ON_USE_SKILL.ordinal(), build.build().toByteArray());
	}

	public void OnLockEnd(IKernel kernel, IGameObject player) {
		player.setProperty(PLAYER_PROPERTY_LOCKFISH, -1);
		player.setProperty(PLAYER_PROPERTY_LOCKEND, 0l);
	}

	public void CancleLock(IKernel kernel, IGameObject player, Object... objects) {
		player.setProperty(PLAYER_PROPERTY_LOCKFISH, -1);
		player.setProperty(PLAYER_PROPERTY_LOCKEND, 0l);
	}
}
