package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.activities.ActivityMgr;
import game.modules.activities.code.JSONResult;
import game.modules.activities.code.ResponseCode;
import game.modules.player.PlayerModule;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 武器模块
 */
public class LevelModule implements ILogicModule {


    private static final String CONFIG_PATH = "res/AllLevels/AllLevels.xml";

    private final Map<Integer, LevelData> m_mapLevel = new HashMap<>();
    private AchievementModule m_AchievementModule;
    private PlayerModule m_playerModule;
    private LittleTargetsModule m_LittleTargetsModule;

    // 限制：必须先开始再结束；结束后 1 秒内不能再次开始
    private static final String TEMP_LEVEL_STARTED = "TempLevelStarted";
    private static final String TEMP_LEVEL_START_LEVEL_ID = "TempLevelStartLevelId";
    private static final String TEMP_LEVEL_IS_SPECIAL = "TempLevelIsSpecial";
    private static final String TEMP_LEVEL_LAST_FINISH_TIME = "TempLevelLastFinishTime";
    private static final long START_AFTER_FINISH_COOLDOWN_MS = 1000L;

    // 关卡完成排行：玩家根据已完成关卡的总等级（PLAYER_LEVELS_COMPLETED）进入排行
    private static final String LEVEL_COMPLETED_RANK_KEY = "LevelCompletedRank";
    private static final int LEVEL_RANK_PAGE_SIZE = 20;
    private static final int LEVEL_RANK_MAX = 100000;

    /**
     * @param kernel
     * @return
     */
    @Override
    public boolean onInit(IKernel kernel) {

        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
        RefreshCfg(kernel, CONFIG_PATH);

        m_AchievementModule = (AchievementModule) kernel.getModule("AchievementModule");
        m_playerModule = kernel.getModule(PlayerModule.class);
        m_LittleTargetsModule = (LittleTargetsModule) kernel.getModule("LittleTargetsModule");

        kernel.regRequestMessage(RequestMsgDef.REQ_LEVEL_START.getId(), this, "OnReqLevelStart");
        kernel.regRequestMessage(RequestMsgDef.REQ_LEVEL_FINISH.getId(), this, "OnReqLevelFinish");
        kernel.regRequestMessage(RequestMsgDef.REQ_LEVEL_RANK_LIST.getId(), this, "OnReqLevelRankList");
        kernel.regRequestMessage(RequestMsgDef.REQ_ACHIEVEMENT_GET_STATUS.getId(), this, "OnReqAchievementGetStatus");

        kernel.regEvent(KernelEvent.KEVENT_OFF_LINE, "Player", this, "OnPlayerOffLine");
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");

        return true;
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
    }

    void RefreshCfg(IKernel kernel, String path) {
        if (path.equals(CONFIG_PATH)) {
            m_mapLevel.clear();
            LoadConfig(kernel, path);
        }
    }


    boolean LoadConfig(IKernel kernel, String path) {
        ICfgReader cfg = kernel.loadXmlConfig(path);
        if (cfg == null) {
            return false;
        }
        int count = cfg.getItemCount();
        for (int i = 0; i < count; ++i) {
            int id = cfg.getInt(i, "Id");
            LevelData data = new LevelData();
            data.id = id;
            data.coin = cfg.getInt(i, "estimatedCoins");
            m_mapLevel.put(id, data);
        }
        return true;
    }

    static class LevelData {
        int id;
        int coin;
    }

    /**
     * 关卡开始：结束后一秒内不能再次开始；且必须先开始后结束。
     * <p>
     * 请求 msg 约定：`CustomMsg.String(value=JSON)`
     * JSON字段：
     * - `levelId`（int）
     * - `isSpecial`（boolean，可选）：特殊关卡不校验前置关卡/跳关限制
     */
    void OnReqLevelStart(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
        long now = kernel.getServerTime();

        try {
            CustomMsg.String msgData = CustomMsg.String.parseFrom(msg);
            JSONObject jsonObject = JSONObject.parseObject(msgData.getValue());
            int levelId = jsonObject.getIntValue("levelId");
            boolean isSpecial = false;
            if (levelId == 999) {
                isSpecial = true;
            }
            if (levelId <= 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            if (!m_mapLevel.containsKey(levelId)) {
                code = ServerCodeDef.CODE_NOT_EXIST;
                return;
            }

            // 结束后 1 秒内不可再开始
            long lastFinishTime = player.getTempLong(TEMP_LEVEL_LAST_FINISH_TIME);
            if (lastFinishTime > 0 && (now - lastFinishTime) < START_AFTER_FINISH_COOLDOWN_MS) {
                code = ServerCodeDef.CODE_TIME_LIMIT;
                return;
            }

            // 已开始则不能重复开始
            if (player.getTempBool(TEMP_LEVEL_STARTED)) {
                code = ServerCodeDef.CODE_WRONG_STATE;
                return;
            }

            // 禁止跳关：只能开始 completed+1 或已完成关卡（重复挑战）
            // 特殊关卡：跳过前置/跳关限制
            if (!isSpecial) {
                int completed = player.getInt(PLAYER_LEVELS_COMPLETED);
                if (levelId > completed + 1) {
                    code = ServerCodeDef.CODE_PARAM_ERR;
                    return;
                }
            }
            player.setTempData(TEMP_LEVEL_STARTED, true);
            player.setTempData(TEMP_LEVEL_START_LEVEL_ID, levelId);
        } finally {
            JSONResult result = ResponseCode.Success.toJSONResult();
            result.setDesc(code);
            // 随机一个小目标，下发给前端（可选）
            if (code == ServerCodeDef.CODE_SUCCESS && m_LittleTargetsModule != null) {
                LittleTargetsModule.LittleTargetData target = m_LittleTargetsModule.onLevelStart(kernel, player);
                if (target != null) {
                    result.put("littleTargetId", target.id);
                    result.put("littleTargetNeed", target.targetValues);
                }
            }
            kernel.response(player, reqid, result);
        }
    }

    // 玩家上线：把当前已通关进度写入排行榜（避免排行榜为空）
    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        updateLevelCompletedRank(kernel, player);
        player.removeTempData(TEMP_LEVEL_LAST_FINISH_TIME);
        player.removeTempData(TEMP_LEVEL_STARTED);
        player.removeTempData(TEMP_LEVEL_START_LEVEL_ID);
    }

    public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
        player.removeTempData(TEMP_LEVEL_LAST_FINISH_TIME);
        player.removeTempData(TEMP_LEVEL_STARTED);
        player.removeTempData(TEMP_LEVEL_START_LEVEL_ID);
    }

    /**
     * 关卡结束并领取金币：前端上报金币，后端按配置上限校验，最多只给上限金币。
     * <p>
     * 请求 msg 约定：`CustomMsg.String(value=JSON)`
     * JSON字段：
     * - `levelId`（int）
     * - `coin`（long）
     * - `isWin`（boolean）
     * - `achievementCounts`（json）
     */
    void OnReqLevelFinish(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        JSONResult result = ResponseCode.Success.toJSONResult();
        ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
        long awardGold = 0;
        long achievementAwardGold = 0;
        long capGold = 0;
        boolean canConsumeStart = false;
        JSONObject achievementStatus = null;
        try {
            if (msg == null || msg.length == 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            CustomMsg.String msgData = CustomMsg.String.parseFrom(msg);
            String jsonStr = msgData.getValue();
            if (jsonStr.trim().isEmpty()) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }

            JSONObject json = JSON.parseObject(jsonStr);
            if (json == null) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }

            Integer levelId = json.getInteger("levelId");
            if (levelId == null || levelId <= 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }

            boolean isSpecial = levelId == 999;
            Long frontcoin = json.getLong("coin");
            if (frontcoin == null || frontcoin < 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            boolean isWin = json.getBoolean("isWin");
            // 只有开始后才能结束
            if (!player.getTempBool(TEMP_LEVEL_STARTED)) {
                code = ServerCodeDef.CODE_WRONG_STATE;
                return;
            }
            int startLevelId = player.getTempInt(TEMP_LEVEL_START_LEVEL_ID);
            if (startLevelId != levelId.intValue()) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }

            LevelData levelData = m_mapLevel.get(levelId);
            if (levelData == null) {
                code = ServerCodeDef.CODE_NOT_EXIST;
                return;
            }

            // 配置 -1 表示不发放金币；按上限逻辑最多只给上限金币
            capGold = levelData.coin > 0 ? (long) levelData.coin : 0;
            awardGold = Math.min(frontcoin, capGold);
            canConsumeStart = true;

            // 小目标进度结算（前端上报本局小目标ID和本局完成量）
            if (m_LittleTargetsModule != null) {
                Integer ltId = json.getInteger("littleTargetId");
                Long ltDelta = json.getLong("littleTargetDelta");
                if (ltId != null && ltDelta != null && ltDelta > 0) {
                    boolean finished = m_LittleTargetsModule.onLevelFinish(kernel, player, ltId, ltDelta);
                    result.put("littleTargetFinished", finished);
                    if (finished) {
                        result.put("littleTargetRewardCoin", (long) LittleTargetsModule.TARGET_COIN);
                    }
                }
            }

            if (awardGold > 0) {
                long curGold = player.getLong(PLAYER_MONEY);
                player.setProperty(PLAYER_MONEY, curGold + awardGold,
                        IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                        "FinishLevel " + levelId + " award=" + awardGold);
            }

            int completed = player.getInt(PLAYER_LEVELS_COMPLETED);
            if (!isSpecial && levelId == completed + 1 && isWin) {
                player.setProperty(PLAYER_LEVELS_COMPLETED, levelId);
                player.setProperty(PLAYER_MAX_LEVEL_INCOME, awardGold);
                // 关卡进度提升后，刷新排行榜分数
                updateLevelCompletedRank(kernel, player);
            }

            // 关卡产出成就（前端上报：当次每个 achievement 的产出数量）
            // achievementOutput: { "achievementId": deltaCount, ... }
            if (m_AchievementModule != null) {
                JSONObject outAch = json.getJSONObject("achievementCounts");
                if (outAch != null) {
                    Map<Integer, Long> totalMap = parseLongMap(player.getString(PLAYER_PROPERTY_ACHIEVEMENT_TOTAL_COUNTS));
                    Map<Integer, Integer> claimedMap = parseIntMap(player.getString(PLAYER_PROPERTY_ACHIEVEMENT_REWARD_CLAIMED_MASKS));

                    achievementStatus = new JSONObject();
                    for (Map.Entry<String, Object> entry : outAch.entrySet()) {
                        if (entry == null || entry.getKey() == null) {
                            continue;
                        }
                        int achievementId;
                        try {
                            achievementId = Integer.parseInt(entry.getKey());
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        Object v = entry.getValue();
                        if (!(v instanceof Number)) {
                            continue;
                        }
                        long delta = ((Number) v).longValue();
                        if (delta <= 0) {
                            continue;
                        }

                        AchievementModule.AchievementData cfg = m_AchievementModule.getAchievementData(achievementId);
                        if (cfg == null) {
                            continue;
                        }

                        long total = totalMap.getOrDefault(achievementId, 0L) + delta;
                        totalMap.put(achievementId, total);

                        int mask = claimedMap.getOrDefault(achievementId, 0);
                        int achievedLv = 0;
                        for (int lvIdx = 0; lvIdx < 5; lvIdx++) {
                            long threshold = cfg.lv[lvIdx];
                            if (threshold <= 0) {
                                continue;
                            }
                            if (total >= threshold && (mask & (1 << lvIdx)) == 0) {
                                long reward = cfg.lvAward[lvIdx];
                                if (reward > 0) {
                                    achievementAwardGold += reward;
                                }
                                mask |= (1 << lvIdx);
                            }
                            if (total >= threshold) {
                                achievedLv = lvIdx + 1;
                            }
                        }
                        claimedMap.put(achievementId, mask);

                        // 组装回包给前端：该 achievement 当前总量/最高达成lv/已领取lv
                        JSONObject one = new JSONObject();
                        one.put("total", total);
                        //one.put("claimedMask", mask);
                        one.put("achievedLv", achievedLv);
//                        java.util.ArrayList<Integer> claimedLv = new java.util.ArrayList<>();
//                        for (int lvIdx = 0; lvIdx < 5; lvIdx++) {
//                            if ((mask & (1 << lvIdx)) != 0) {
//                                claimedLv.add(lvIdx + 1);
//                            }
//                        }
//                        one.put("claimedLv", claimedLv);
                        achievementStatus.put(String.valueOf(achievementId), one);
                    }

                    if (!totalMap.isEmpty()) {
                        player.setProperty(PLAYER_PROPERTY_ACHIEVEMENT_TOTAL_COUNTS, joinLongMap(totalMap));
                    }
                    if (!claimedMap.isEmpty()) {
                        player.setProperty(PLAYER_PROPERTY_ACHIEVEMENT_REWARD_CLAIMED_MASKS, joinIntMap(claimedMap));
                    }
                    if (achievementAwardGold > 0) {
                        long curGold = player.getLong(PLAYER_MONEY);
                        player.setProperty(PLAYER_MONEY, curGold + achievementAwardGold,
                                IKernel.PlayerLogType.PROP_CHANGE.ordinal(), "AchievementReward");
                    }
                }
            }
        } finally {
            if (canConsumeStart) {
                long now = kernel.getServerTime();
                player.setTempData(TEMP_LEVEL_STARTED, false);
                player.setTempData(TEMP_LEVEL_START_LEVEL_ID, 0);
                player.setTempData(TEMP_LEVEL_IS_SPECIAL, false);
                player.setTempData(TEMP_LEVEL_LAST_FINISH_TIME, now);
            }

            result.setDesc(code);
            result.put("awardGold", awardGold);
            result.put("achievementAwardGold", achievementAwardGold);
            if (achievementStatus != null) {
                result.put("achievementStatus", achievementStatus);
            }
            kernel.response(player, reqid,
                    CustomMsg.String.newBuilder().setValue(result.toJSONString()).build().toByteArray());
        }
    }

    /**
     * 关卡排行列表
     * 前端传入：CustomMsg.Int32 页数 page（每页 20 条）
     */
    void OnReqLevelRankList(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) throws InvalidProtocolBufferException {
        ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
        int page = 1;
        try {
            if (msg == null || msg.length == 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                JSONObject resp = new JSONObject();
                resp.put("code", code.ordinal());
                resp.put("desc", code.getDesc());
                kernel.response(player, reqid,
                        CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
                return;
            }

            CustomMsg.Int32 msgData = CustomMsg.Int32.parseFrom(msg);
            page = msgData.getValue();

            if (page < 1) {
                page = 1;
            }

            int start = (page - 1) * LEVEL_RANK_PAGE_SIZE;
            int end = start + LEVEL_RANK_PAGE_SIZE - 1;

            Jedis jedis = kernel.getJedis();
            if (jedis == null) {
                code = ServerCodeDef.CODE_FAILED;
                JSONObject resp = new JSONObject();
                resp.put("code", code.ordinal());
                resp.put("desc", code.getDesc());
                kernel.response(player, reqid,
                        CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
                return;
            }

            String key = LEVEL_COMPLETED_RANK_KEY;
            long total = jedis.zcard(key);

            List<Tuple> rankList = jedis.zrevrangeWithScores(key, start, end);
            List<JSONObject> result = new ArrayList<>();
            for (int i = 0; i < rankList.size(); i++) {
                Tuple tuple = rankList.get(i);
                if (tuple == null || tuple.getElement() == null) {
                    continue;
                }
                int uid = 0;
                try {
                    uid = Integer.parseInt(tuple.getElement());
                } catch (NumberFormatException ignored) {
                }
                if (uid <= 0) {
                    continue;
                }

                long score = (long) tuple.getScore();
                int rank = start + i + 1;

                JSONObject info = null;
                if (m_playerModule != null) {
                    info = m_playerModule.getPlayerRedisPubInfo(kernel, String.valueOf(uid));
                }
                if (info == null) {
                    info = new JSONObject();
                    info.put(PLAYER_PROPERTY_UID, uid);
                    info.put(PLAYER_PROPERTY_NAME, kernel.getUserName(uid));
                    info.put(PLAYER_PROPERTY_HEADID, kernel.getUserHeadid(uid));
                }
                info.put("rank", rank);
                info.put("score", score);
                result.add(info);
            }

            JSONObject resp = new JSONObject();
            resp.put("list", result);
            resp.put("page", page);
            resp.put("pageSize", LEVEL_RANK_PAGE_SIZE);
            resp.put("total", total);

            kernel.response(player, reqid,
                    CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
        } catch (Exception e) {
            code = ServerCodeDef.CODE_FAILED;
            JSONObject resp = new JSONObject();
            resp.put("code", code.ordinal());
            resp.put("desc", code.getDesc());
            kernel.response(player, reqid,
                    CustomMsg.String.newBuilder().setValue(resp.toJSONString()).build().toByteArray());
        }
    }

    /**
     * 获取成就领取状态（不发奖励，仅返回进度）
     * 返回字段：achievementStatus[achievementId] = {total, claimedMask, achievedLv, claimedLv}
     */
    void OnReqAchievementGetStatus(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        ServerCodeDef code = game.custommsg.ServerCodeDef.CODE_SUCCESS;
        JSONObject achievementStatus = new JSONObject();

        try {
            if (m_AchievementModule == null) {
                code = ServerCodeDef.CODE_NOT_EXIST;
                achievementStatus = new JSONObject();
            } else {
                Map<Integer, Long> totalMap = parseLongMap(player.getString(PLAYER_PROPERTY_ACHIEVEMENT_TOTAL_COUNTS));
                Map<Integer, Integer> claimedMap = parseIntMap(player.getString(PLAYER_PROPERTY_ACHIEVEMENT_REWARD_CLAIMED_MASKS));

                for (Integer achievementId : m_AchievementModule.getAllAchievementIds()) {
                    if (achievementId == null) {
                        continue;
                    }
                    AchievementModule.AchievementData cfg = m_AchievementModule.getAchievementData(achievementId);
                    if (cfg == null) {
                        continue;
                    }

                    long total = totalMap.getOrDefault(achievementId, 0L);
                    int mask = claimedMap.getOrDefault(achievementId, 0);

                    int achievedLv = 0;
                    //java.util.ArrayList<Integer> claimedLv = new java.util.ArrayList<>();
                    for (int lvIdx = 0; lvIdx < 5; lvIdx++) {
                        long threshold = cfg.lv[lvIdx];
                        if (threshold > 0 && total >= threshold) {
                            achievedLv = lvIdx + 1;
                        }
//                        if ((mask & (1 << lvIdx)) != 0) {
//                            claimedLv.add(lvIdx + 1);
//                        }
                    }

                    JSONObject one = new JSONObject();
                    one.put("total", total);
                    one.put("achievedLv", achievedLv);
                    //one.put("claimedLv", claimedLv);
                    achievementStatus.put(String.valueOf(achievementId), one);
                }
            }
        } catch (Exception e) {
            code = ServerCodeDef.CODE_FAILED;
            achievementStatus = new JSONObject();
        }

        JSONResult result = ResponseCode.Success.toJSONResult();
        result.setDesc(code);
        result.put("achievementStatus", achievementStatus);
        kernel.response(player, reqid,
                CustomMsg.String.newBuilder().setValue(result.toJSONString()).build().toByteArray());
    }

    private void updateLevelCompletedRank(IKernel kernel, IGameObject player) {
        try {
            Jedis jedis = kernel.getJedis();
            if (jedis == null || player == null) {
                return;
            }
            int uid = player.getInt(PLAYER_PROPERTY_UID);
            if (uid <= 0) {
                return;
            }
            long score = player.getInt(PLAYER_LEVELS_COMPLETED);
            if (score < 0) {
                score = 0;
            }

            jedis.zadd(LEVEL_COMPLETED_RANK_KEY, score, String.valueOf(uid));

            // 保留前 LEVEL_RANK_MAX 名
            Long currentCount = jedis.zcard(LEVEL_COMPLETED_RANK_KEY);
            while (currentCount != null && currentCount > LEVEL_RANK_MAX) {
                // 删除第 (LEVEL_RANK_MAX+1) 名（按 zrevrange 的下标从 0 开始）
                java.util.List<String> removeList = jedis.zrevrange(LEVEL_COMPLETED_RANK_KEY, LEVEL_RANK_MAX, LEVEL_RANK_MAX);
                if (removeList == null || removeList.isEmpty()) {
                    break;
                }
                String uidToRemove = removeList.iterator().next();
                if (uidToRemove == null) {
                    break;
                }
                jedis.zrem(LEVEL_COMPLETED_RANK_KEY, uidToRemove);
                currentCount--;
            }
        } catch (Exception ignored) {
        }
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

    private static Map<Integer, Integer> parseIntMap(String s) {
        Map<Integer, Integer> map = new HashMap<>();
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
                int v = Integer.parseInt(kv[1].trim());
                map.put(k, v);
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private static String joinIntMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        java.util.ArrayList<Integer> keys = new java.util.ArrayList<>(map.keySet());
        keys.sort(Integer::compareTo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            int k = keys.get(i);
            int v = map.getOrDefault(k, 0);
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(k).append('=').append(v);
        }
        return sb.toString();
    }
}
