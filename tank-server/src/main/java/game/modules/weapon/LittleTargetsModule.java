package game.modules.weapon;

import framework.game.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 小目标模块
 */
public class LittleTargetsModule implements ILogicModule {
    private static final String CONFIG_PATH = "res/LittleTargets/LTtargetValues.xml";
    public static final int TARGET_COIN = 50;
    private final Map<Integer, LittleTargetData> m_mapLittleTarget = new HashMap<>();

    // 玩家属性名
    private static final String PLAYER_PROPERTY_LITTLE_TARGET_CURRENT_ID = "LittleTargetCurrentId";
    // 累计进度：用与成就类似的 "id=value;id=value" 字符串保存
    private static final String PLAYER_PROPERTY_LITTLE_TARGET_TOTAL_COUNTS = "LittleTargetTotalCounts";

    private final Random m_rand = new Random();

    static class LittleTargetData {
        int id; // xml里的 TargetTypes
        int targetValues; // xml里的 targetValues
    }

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        RefreshCfg(kernel, CONFIG_PATH);
        return true;
    }

    @Override
    public void onDestroy() {
    }

    /**
     * 声明玩家字段：当前小目标ID + 各目标累计进度
     */
    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_LITTLE_TARGET_CURRENT_ID, ValueType.INT, false, false, true);
        kernel.declareProperty(script, PLAYER_PROPERTY_LITTLE_TARGET_TOTAL_COUNTS, ValueType.STRING, false, false, true);
    }

    void RefreshCfg(IKernel kernel, String path) {
        if (!CONFIG_PATH.equals(path)) {
            return;
        }
        m_mapLittleTarget.clear();
        LoadConfig(kernel, path);
    }

    boolean LoadConfig(IKernel kernel, String path) {
        ICfgReader cfg = kernel.loadXmlConfig(path);
        if (cfg == null) {
            return false;
        }
        int count = cfg.getItemCount();
        for (int i = 0; i < count; ++i) {
            LittleTargetData data = new LittleTargetData();
            data.id = cfg.getInt(i, "TargetTypes");
            data.targetValues = cfg.getInt(i, "targetValues");
            m_mapLittleTarget.put(data.id, data);
        }
        return true;
    }

    /**
     * 关卡开始时调用：为玩家随机一个小目标，并记录到玩家属性。
     * <p>
     * 规则：
     * - 从所有配置的 TargetTypes 中随机一个
     * - 写入 PLAYER_PROPERTY_LITTLE_TARGET_CURRENT_ID
     * - 返回目标ID和目标总需求（给前端显示）
     */
    public LittleTargetData onLevelStart(IKernel kernel, IGameObject player) {
        if (m_mapLittleTarget.isEmpty() || player == null) {
            return null;
        }
        // 随机一个目标类型
        Object[] keys = m_mapLittleTarget.keySet().toArray();
        int idx = m_rand.nextInt(keys.length);
        int targetId = (Integer) keys[idx];
        LittleTargetData cfg = m_mapLittleTarget.get(targetId);
        if (cfg == null) {
            return null;
        }
        player.setProperty(PLAYER_PROPERTY_LITTLE_TARGET_CURRENT_ID, targetId);
        return cfg;
    }

    /**
     * 关卡结束时调用：前端上报本局小目标的完成量，累计到玩家总进度。
     *
     * @param player           玩家
     * @param reportedTargetId 前端上报的小目标ID（TargetTypes）
     * @param deltaProgress    本局完成量
     * @return 本次是否完成了小目标（如果完成会清零该目标进度并返回 true）
     */
    public boolean onLevelFinish(IKernel kernel, IGameObject player, int reportedTargetId, long deltaProgress) {
        if (player == null) {
            return false;
        }
        LittleTargetData cfg = m_mapLittleTarget.get(reportedTargetId);
        if (cfg == null || cfg.targetValues <= 0) {
            return false;
        }
        if (deltaProgress <= 0) {
            return false;
        }

        Map<Integer, Long> totalMap = parseLongMap(player.getString(PLAYER_PROPERTY_LITTLE_TARGET_TOTAL_COUNTS));
        long oldVal = totalMap.getOrDefault(reportedTargetId, 0L);
        long newVal = oldVal + deltaProgress;

        boolean finished = false;
        if (newVal >= cfg.targetValues) {
            // 完成目标：奖励50金币，并清零该目标进度
            finished = true;
            totalMap.put(reportedTargetId, newVal - cfg.targetValues);
            long curGold = player.getLong(PLAYER_MONEY);
            player.setProperty(PLAYER_MONEY, curGold + TARGET_COIN,
                    IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                    "LittleTarget " + reportedTargetId + " finish +" + TARGET_COIN);
        } else {
            totalMap.put(reportedTargetId, newVal);
        }

        player.setProperty(PLAYER_PROPERTY_LITTLE_TARGET_TOTAL_COUNTS, joinLongMap(totalMap));
        // 本局结束后，当前小目标重置为空（下次关卡重新随机）
        player.setProperty(PLAYER_PROPERTY_LITTLE_TARGET_CURRENT_ID, 0);
        return finished;
    }

    private static Map<Integer, Long> parseLongMap(String s) {
        Map<Integer, Long> map = new HashMap<>();
        if (s == null) {
            return map;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return map;
        }
        String[] pairs = s.split(";");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            try {
                int k = Integer.parseInt(kv[0].trim());
                long v = Long.parseLong(kv[1].trim());
                map.put(k, v);
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private static String joinLongMap(Map<Integer, Long> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        java.util.ArrayList<Integer> keys = new java.util.ArrayList<>(map.keySet());
        keys.sort(Integer::compareTo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            int k = keys.get(i);
            long v = map.getOrDefault(k, 0L);
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(k).append('=').append(v);
        }
        return sb.toString();
    }
}
