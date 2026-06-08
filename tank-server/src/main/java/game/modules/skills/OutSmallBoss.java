package game.modules.skills;

import java.util.Random;

import framework.game.IRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import game.custommsg.CustomMsg;
import game.custommsg.S2CMsgDef;
import game.modules.fishgame.FishModule;
import game.modules.fishgame.FishModule.FishData;
import game.modules.utils.UtilFunc;

class OutSmallBossData extends BaseSkillData {
	int time; // 未用
	String fishId;
}

/**
 * 描述： 4,5,6点骰子的召唤小型boss（30秒）
 */
public class OutSmallBoss extends BaseSkill {

	private static Logger logger = LoggerFactory.getLogger(OutSmallBoss.class);

	FishModule m_FishModule = null;

	public boolean OnInit(IKernel kernel) {
		m_FishModule = (FishModule) kernel.getModule("FishModule");
		return true;
	}

	BaseSkillData LoadConfig(ICfgReader cfg, int index) {
		OutSmallBossData data = new OutSmallBossData();
		data.type = m_type;
		data.time = cfg.getInt(index, "Time");
		data.fishId = cfg.getString(index, "FishID");

		return data;
	}

	public void OnUse(IKernel kernel, IGameObject player, Object target, BaseSkillData skillData) {
//		OutSmallBossData data = (OutSmallBossData) skillData;
//		// boolean haveBoss = false;
//
//		CustomMsg.OutFish.Builder outFish = CustomMsg.OutFish.newBuilder();
//		outFish.setCount(1);
//		FishData smallBossData = m_FishModule.GetFishData(data.fishId);
//		outFish.addFishtype(smallBossData.enumType);
//		outFish.addSurvivalTime(smallBossData.survivalTime);
//		Random random = new Random();
//		int[] pathIds = smallBossData.path;
//		int randomInt = random.nextInt(pathIds.length);
//		outFish.addPathid(pathIds[randomInt]);
//
//		IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
//		if (desk == null) {
//			return;
//		}
//		int lastFishIndex = desk.getInt(DESK_FISH_INDEX);
//		outFish.addFishindex(lastFishIndex);
//		desk.setProperty(DESK_FISH_INDEX, lastFishIndex + 1);
//		// 检查不要与boss冲突
//		IRecord rec = desk.getRecord(DESK_FISH_LIST);
//		//float pathLength = m_OutFishModule.pathPointMap.get(pathIds[randomInt]);
//		int lifeTime = smallBossData.survivalTime * 1000;
//		rec.addRow(lastFishIndex, smallBossData.cfgid, false, -1, 1, kernel.getServerTime() + lifeTime,0L);
//		IRecord fishCountRec = desk.getRecord(DESK_FISH_COUNT);
//		UtilFunc.addOneFish(smallBossData.type, fishCountRec);
//		kernel.broadCastByDesk(desk, S2CMsgDef.S2C_OUT_FISH.ordinal(), outFish.build().toByteArray());
	}
}
