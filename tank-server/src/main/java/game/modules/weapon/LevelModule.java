package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.SystemConfigData;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 武器模块
 */
public class LevelModule implements ILogicModule {
    private static final Logger logger = LoggerFactory.getLogger(LevelModule.class);

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
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");

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

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, LevelAttemptState.PROPERTY, ValueType.STRING, false, false, true);
        kernel.declareProperty(script, PLAYER_PROGRESS_REVISION, ValueType.INT, false, true, true);
    }

    /**
     * 关卡开始：结束后一秒内不能再次开始；且必须先开始后结束。
     * <p>
     * 请求 msg 约定：`CustomMsg.String(value=JSON)`
     * JSON字段：
     * - `levelId`（int）
     * - `settlementVersion`（int，可选）：2 表示结算必须携带 attemptId
     * 特殊关卡仅由服务端识别 levelId=999，不接受客户端绕过跳关限制。
     */
    void OnReqLevelStart(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        ServerCodeDef code = ServerCodeDef.CODE_SUCCESS;
        long now = kernel.getServerTime();
        JSONResult result = ResponseCode.Success.toJSONResult();
        boolean save = false;

        try {
            CustomMsg.String msgData = CustomMsg.String.parseFrom(msg);
            JSONObject jsonObject = JSONObject.parseObject(msgData.getValue());
            if (jsonObject == null) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            int settlementVersion = LevelAttemptState.settlementVersion(jsonObject);
            if (requiresV2() && settlementVersion != 2) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                result.put("status", "settlement_upgrade_required");
                return;
            }
            if (!LevelAttemptState.integer(jsonObject.get("levelId"))
                    || jsonObject.getLongValue("levelId") > Integer.MAX_VALUE || jsonObject.getLongValue("levelId") <= 0) {
                throw new LevelAttemptState.ContractException("invalid_level_id");
            }
            int levelId = jsonObject.getIntValue("levelId");
            String gameplayMode = LevelAttemptState.gameplayMode(jsonObject, levelId);
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
            LevelAttemptState state = new LevelAttemptState(player.getString(LevelAttemptState.PROPERTY));
            long lastFinishTime = Math.max(state.data.getLongValue("lastFinishTime"),
                    player.getTempLong(TEMP_LEVEL_LAST_FINISH_TIME));
            if (lastFinishTime > 0 && (now - lastFinishTime) < START_AFTER_FINISH_COOLDOWN_MS) {
                code = ServerCodeDef.CODE_TIME_LIMIT;
                result.put("status", "level_restart_cooldown");
                result.put("retryAfterMs", START_AFTER_FINISH_COOLDOWN_MS - (now - lastFinishTime));
                result.put("nextStartAt", lastFinishTime + START_AFTER_FINISH_COOLDOWN_MS);
                result.put("retryable", true);
                return;
            }

            // 重连/开始回包丢失时返回原凭据，不重新随机小目标。
            JSONObject active = state.active();
            if (active != null && now - active.getLongValue("startedAt") < LevelAttemptState.MAX_AGE_MS
                    && Math.max(1, active.getIntValue("progressRevision"))
                    == Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION))) {
                if (active.getIntValue("levelId") == levelId) {
                    // Upgrading the client does not create another attempt or reroll its target.
                    if (settlementVersion == 2) {
                        active.put("requiresAttemptId", true);
                        active.put("requiresResolution", true);
                    }
                    JSONObject originalStart = active.getJSONObject("startResponse");
                    originalStart.put("settlementVersion", active.getBooleanValue("requiresAttemptId") ? 2 : 1);
                    originalStart.put("gameplayMode", gameplayMode);
                    originalStart.put("attemptExpiresAt", active.getLongValue("startedAt") + LevelAttemptState.MAX_AGE_MS);
                    if (!originalStart.containsKey("littleTargetAssigned")) {
                        originalStart.put("littleTargetAssigned", originalStart.containsKey("littleTargetId"));
                    }
                    player.setProperty(LevelAttemptState.PROPERTY, state.data.toJSONString());
                    result.putAll(active.getJSONObject("startResponse"));
                    result.put("resumed", true);
                    save = true;
                    return;
                }
                code = ServerCodeDef.CODE_WRONG_STATE;
                result.put("status", "unfinished_level");
                result.put("attemptId", active.getString("attemptId"));
                result.put("levelId", active.getIntValue("levelId"));
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
            String attemptId = UUID.randomUUID().toString();
            result.put("attemptId", attemptId);
            result.put("levelId", levelId);
            result.put("resumed", false);
            result.put("settlementVersion", settlementVersion);
            result.put("gameplayMode", gameplayMode);
            result.put("attemptExpiresAt", now + LevelAttemptState.MAX_AGE_MS);
            result.put("progressRevision", Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION)));
            result.put("littleTargetAssigned", false);
            result.put("littleTargetId", 0);
            result.put("littleTargetNeed", 0);
            if (m_LittleTargetsModule != null) {
                LittleTargetsModule.LittleTargetData target = m_LittleTargetsModule.onLevelStart(kernel, player);
                if (target != null) {
                    result.put("littleTargetAssigned", true);
                    result.put("littleTargetId", target.id);
                    result.put("littleTargetNeed", target.targetValues);
                }
            }
            active = new JSONObject();
            active.put("attemptId", attemptId);
            active.put("levelId", levelId);
            active.put("startedAt", now);
            active.put("progressRevision", Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION)));
            // 新客户端可选择严格模式；旧客户端仍可不带 attemptId 结算。
            active.put("requiresAttemptId", settlementVersion == 2);
            active.put("requiresResolution", settlementVersion == 2);
            active.put("startResponse", JSON.parseObject(result.toJSONString()));
            state.data.put("active", active);
            player.setProperty(LevelAttemptState.PROPERTY, state.data.toJSONString());
            save = true;
        } catch (Exception e) {
            logger.warn("level start rejected uid={}", player.getInt(PLAYER_PROPERTY_UID), e);
            code = ServerCodeDef.CODE_PARAM_ERR;
            if (e instanceof LevelAttemptState.ContractException) {
                result.put("status", ((LevelAttemptState.ContractException) e).status);
            }
        } finally {
            result.setDesc(code);
            if (!result.containsKey("status")) {
                result.put("status", code == ServerCodeDef.CODE_SUCCESS ? "level_started" : "level_start_rejected");
            }
            if (save && code == ServerCodeDef.CODE_SUCCESS) {
                respondAfterSave(kernel, player, reqid, result);
            } else {
                kernel.response(player, reqid, result);
            }
        }
    }

    private static boolean requiresV2() {
        Object configured = SystemConfigData.getConfig("levelSettlementRequireV2", (Object) false);
        return Boolean.parseBoolean(String.valueOf(configured));
    }

    // 玩家上线：把当前已通关进度写入排行榜（避免排行榜为空）
    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        if (player.getInt(PLAYER_PROGRESS_REVISION) <= 0) {
            player.setProperty(PLAYER_PROGRESS_REVISION, 1);
        }
        updateLevelCompletedRank(kernel, player);
        // 保存的关卡凭据和回执跨断线/重登保留；临时字段只用于兼容旧连接。
        player.removeTempData(TEMP_LEVEL_STARTED);
        player.removeTempData(TEMP_LEVEL_START_LEVEL_ID);
    }

    public void OnPlayerOffLine(IKernel kernel, IGameObject player) {
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
        LevelAttemptState state = null;
        String attemptId = null;
        int finishedLevelId = 0;
        boolean replay = false;
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

            if (!LevelAttemptState.integer(json.get("levelId")) || json.getLongValue("levelId") > Integer.MAX_VALUE
                    || json.getLongValue("levelId") <= 0) {
                throw new LevelAttemptState.ContractException("invalid_level_id");
            }
            Integer levelId = json.getInteger("levelId");
            if (levelId == null || levelId <= 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            LevelAttemptState.gameplayMode(json, levelId);

            boolean isSpecial = levelId == 999;
            Long frontcoin = json.getLong("coin");
            if (frontcoin == null || frontcoin < 0) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            boolean isWin = LevelAttemptState.resolveWin(json);
            state = new LevelAttemptState(player.getString(LevelAttemptState.PROPERTY));
            attemptId = json.getString("attemptId");
            if (attemptId != null && attemptId.length() > 128) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                return;
            }
            JSONObject receipt = state.receipt(attemptId);
            if (receipt != null) {
                if (receipt.getIntValue("levelId") != levelId) {
                    code = ServerCodeDef.CODE_PARAM_ERR;
                    return;
                }
                result.putAll(receipt);
                result.put("duplicate", true);
                replay = true;
                return;
            }
            JSONObject active = state.active();
            int startLevelId;
            if (active != null) {
                if (requiresV2() && !active.getBooleanValue("requiresAttemptId")) {
                    code = ServerCodeDef.CODE_PARAM_ERR;
                    result.put("status", "settlement_upgrade_required");
                    return;
                }
                int revision = Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION));
                if (Math.max(1, active.getIntValue("progressRevision")) != revision) {
                    code = ServerCodeDef.CODE_WRONG_STATE;
                    result.put("status", "progress_revision_changed");
                    result.put("progressRevision", revision);
                    return;
                }
                if (active.getBooleanValue("requiresAttemptId") && (attemptId == null || attemptId.isEmpty())) {
                    code = ServerCodeDef.CODE_PARAM_ERR;
                    result.put("status", "attempt_id_required");
                    return;
                }
                if (active.getBooleanValue("requiresResolution")) {
                    String resolution = LevelAttemptState.validateStrictFinish(json, active);
                    json.put("resolution", resolution);
                }
                if ((active.getBooleanValue("requiresAttemptId") || attemptId != null)
                        && !active.getString("attemptId").equals(attemptId)) {
                    code = ServerCodeDef.CODE_WRONG_STATE;
                    result.put("status", "attempt_mismatch");
                    return;
                }
                if (kernel.getServerTime() - active.getLongValue("startedAt") >= LevelAttemptState.MAX_AGE_MS) {
                    code = ServerCodeDef.CODE_WRONG_STATE;
                    result.put("status", "attempt_expired");
                    return;
                }
                attemptId = active.getString("attemptId");
                startLevelId = active.getIntValue("levelId");
            } else if ((attemptId == null || attemptId.isEmpty()) && player.getTempBool(TEMP_LEVEL_STARTED)) {
                if (requiresV2()) {
                    code = ServerCodeDef.CODE_PARAM_ERR;
                    result.put("status", "settlement_upgrade_required");
                    return;
                }
                // 仅兼容部署前已经开始的一局，不能凭客户端任意构造的 ID 补结算。
                attemptId = UUID.randomUUID().toString();
                startLevelId = player.getTempInt(TEMP_LEVEL_START_LEVEL_ID);
            } else {
                code = ServerCodeDef.CODE_WRONG_STATE;
                result.put("status", "attempt_not_found");
                return;
            }
            if (startLevelId != levelId.intValue()) {
                code = ServerCodeDef.CODE_PARAM_ERR;
                result.put("status", "level_id_mismatch");
                return;
            }

            LevelData levelData = m_mapLevel.get(levelId);
            if (levelData == null) {
                code = ServerCodeDef.CODE_NOT_EXIST;
                return;
            }

            // 配置 -1 表示不发放金币；按上限逻辑最多只给上限金币
            capGold = levelData.coin > 0 ? (long) levelData.coin : 0;
            boolean noEarnings = active != null && active.getBooleanValue("requiresResolution")
                    && ("Restart".equals(json.getString("resolution")) || "Aborted".equals(json.getString("resolution")));
            awardGold = noEarnings ? 0 : Math.min(frontcoin, capGold);
            canConsumeStart = true;
            finishedLevelId = levelId;
            result.put("isWin", isWin);
            result.put("resolution", json.containsKey("resolution") ? json.getString("resolution") : "legacy");
            result.put("reportedIsWin", json.getBoolean("isWin"));
            result.put("earningsSuppressed", noEarnings);
            if (json.containsKey("countProgress")) {
                result.put("countProgress", json.getBoolean("countProgress"));
            }
            result.put("levelsCompletedBefore", player.getInt(PLAYER_LEVELS_COMPLETED));
            result.put("balanceBefore", player.getLong(PLAYER_MONEY));

            // 小目标进度结算（前端上报本局小目标ID和本局完成量）
            if (!noEarnings && m_LittleTargetsModule != null) {
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
                player.setProperty(PLAYER_MONEY, Math.addExact(curGold, awardGold),
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
            if (!noEarnings && m_AchievementModule != null) {
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

                        long total = Math.addExact(totalMap.getOrDefault(achievementId, 0L), delta);
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
                                    achievementAwardGold = Math.addExact(achievementAwardGold, reward);
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
                        player.setProperty(PLAYER_MONEY, Math.addExact(curGold, achievementAwardGold),
                                IKernel.PlayerLogType.PROP_CHANGE.ordinal(), "AchievementReward");
                    }
                }
            }
        } catch (Exception e) {
            if (canConsumeStart) {
                logger.error("level finish failed uid={} attempt={}", player.getInt(PLAYER_PROPERTY_UID), attemptId, e);
            } else {
                logger.warn("invalid level finish uid={}: {}", player.getInt(PLAYER_PROPERTY_UID), e.toString());
            }
            code = canConsumeStart ? ServerCodeDef.CODE_FAILED : ServerCodeDef.CODE_PARAM_ERR;
            result.put("status", canConsumeStart ? "settlement_requires_review" : "invalid_request");
            if (e instanceof LevelAttemptState.ContractException) {
                result.put("status", ((LevelAttemptState.ContractException) e).status);
            }
            result.put("retryable", false);
            if (canConsumeStart) {
                result.put("manualReview", true);
            }
        } finally {
            if (replay) {
                respondAfterSave(kernel, player, reqid, result);
            } else {
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
                if (!result.containsKey("status")) {
                    result.put("status", code == ServerCodeDef.CODE_SUCCESS ? "level_settled" : "level_finish_rejected");
                }
                if (canConsumeStart) {
                    result.put("attemptId", attemptId);
                    result.put("levelId", finishedLevelId);
                    result.put("duplicate", false);
                    long settledAt = kernel.getServerTime();
                    result.put("nextStartAt", settledAt + START_AFTER_FINISH_COOLDOWN_MS);
                    result.put("progressRevision", Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION)));
                    result.put("levelsCompletedAfter", player.getInt(PLAYER_LEVELS_COMPLETED));
                    result.put("balanceAfter", player.getLong(PLAYER_MONEY));
                    state.finish(result, settledAt);
                    player.setProperty(LevelAttemptState.PROPERTY, state.data.toJSONString());
                    respondAfterSave(kernel, player, reqid, result);
                } else {
                    kernel.response(player, reqid,
                            CustomMsg.String.newBuilder().setValue(result.toJSONString()).build().toByteArray());
                }
            }
        }
    }

    private void respondAfterSave(IKernel kernel, IGameObject player, int reqid, JSONObject response) {
        // Reuse the existing bounded, acknowledged player archive flush. The receipt and money
        // are in one archive; a lost response can safely be retried with the same attemptId.
        JSONObject snapshot = JSON.parseObject(response.toJSONString());
        kernel.flushGooglePlayPlayer(player, saved -> {
            JSONObject reply = JSON.parseObject(snapshot.toJSONString());
            reply.put("progressRevision", Math.max(1, player.getInt(PLAYER_PROGRESS_REVISION)));
            reply.put("levelsCompleted", player.getInt(PLAYER_LEVELS_COMPLETED));
            reply.put("retryable", false);
            if (reply.containsKey("nextStartAt")) {
                reply.put("retryAfterMs", Math.max(0L, reply.getLongValue("nextStartAt") - kernel.getServerTime()));
            }
            if (!Boolean.TRUE.equals(saved)) {
                reply.put("code", ServerCodeDef.CODE_FAILED.getId());
                reply.put("status", "player_save_pending");
                reply.put("retryable", true);
            }
            logger.info("level archive ack uid={} attemptId={} levelId={} status={} saved={} duplicate={} "
                            + "resolution={} isWin={} awardGold={} reqid={} code={} progressRevision={} "
                            + "reportedIsWin={} countProgress={} levelsCompletedBefore={} levelsCompletedAfter={} "
                            + "balanceBefore={} balanceAfter={}", player.getInt(PLAYER_PROPERTY_UID),
                    reply.getString("attemptId"), reply.getInteger("levelId"), reply.getString("status"),
                    saved, reply.getBoolean("duplicate"), reply.getString("resolution"),
                    reply.getBoolean("isWin"), reply.getLong("awardGold"), reqid, reply.getInteger("code"),
                    reply.getInteger("progressRevision"), reply.getBoolean("reportedIsWin"),
                    reply.getBoolean("countProgress"), reply.getInteger("levelsCompletedBefore"),
                    reply.getInteger("levelsCompletedAfter"), reply.getLong("balanceBefore"),
                    reply.getLong("balanceAfter"));
            kernel.response(player, reqid, CustomMsg.String.newBuilder()
                    .setValue(reply.toJSONString()).build().toByteArray());
        });
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
