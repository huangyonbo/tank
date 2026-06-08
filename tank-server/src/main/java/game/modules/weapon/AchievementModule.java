package game.modules.weapon;

import framework.game.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 武器模块
 */
public class AchievementModule implements ILogicModule {
	private static final String CONFIG_PATH = "res/Achievement/Achievement.xml";

	// key: xml里的 Achievement 字段（0..n）
	private final Map<Integer, AchievementData> m_mapAchievement = new HashMap<>();

	static class AchievementData {
		int id; // xml里的 Id
		int achievement; // xml里的 Achievement
		int[] lv = new int[5]; // lv1..lv5 达成门槛
		int[] lvAward = new int[5]; // lv1Award..lv5Award 奖励
	}

	@Override
	public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");

		kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
		RefreshCfg(kernel, CONFIG_PATH);
		return true;
	}

	@Override
	public void onDestroy() {
	}

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_MAX_LEVEL_INCOME, ValueType.LONG, true, false, true);
        kernel.declareProperty(script, PLAYER_LEVELS_COMPLETED, ValueType.INT, true, false, true);
        kernel.declareProperty(script, PLAYER_MONEY, ValueType.LONG, true, false, true);
        kernel.declareProperty(script, PLAYER_WEAPONS_LEVELS, ValueType.STRING, true, false, true);
        kernel.declareProperty(script, PLAYER_TANK_SPEED_LEVEL, ValueType.INT, true, false, true);
        kernel.declareProperty(script, PLAYER_TANK_ARMOR_LEVEL, ValueType.INT, true, false, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_ACHIEVEMENT_TOTAL_COUNTS, ValueType.STRING, false, false, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_ACHIEVEMENT_REWARD_CLAIMED_MASKS, ValueType.STRING, false, false, true);
	}

    void RefreshCfg(IKernel kernel, String path) {
		if (!CONFIG_PATH.equals(path)) {
			return;
		}
		m_mapAchievement.clear();
		LoadConfig(kernel, path);
	}

	boolean LoadConfig(IKernel kernel, String path) {
		ICfgReader cfg = kernel.loadXmlConfig(path);
		if (cfg == null) {
			return false;
		}
		int count = cfg.getItemCount();
		for (int i = 0; i < count; ++i) {
			AchievementData data = new AchievementData();
			data.id = cfg.getInt(i, "Id");
			data.achievement = cfg.getInt(i, "Achievement");
			for (int lvIdx = 1; lvIdx <= 5; lvIdx++) {
				data.lv[lvIdx - 1] = cfg.getInt(i, "lv" + lvIdx);
				data.lvAward[lvIdx - 1] = cfg.getInt(i, "lv" + lvIdx + "Award");
			}
			m_mapAchievement.put(data.achievement, data);
		}
		return true;
	}

	// 后续业务可以调用：获取某个 Achievement 的配表数据
	AchievementData getAchievementData(int achievement) {
		return m_mapAchievement.get(achievement);
	}

	// 获取所有成就 AchievementId（用于前端拉取领取状态）
	java.util.Set<Integer> getAllAchievementIds() {
		return new java.util.HashSet<>(m_mapAchievement.keySet());
	}
}
