package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import framework.SystemConfigData;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.KernelEvent;
import framework.game.ValueType;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Calendar;

public class AdModule implements ILogicModule {
    private static final Logger logger = LoggerFactory.getLogger(AdModule.class);

    private static final String PROPERTY_REWARDED_AD_DAY = "RewardedAdDay";
    private static final String PROPERTY_REWARDED_AD_DAILY_COUNT = "RewardedAdDailyCount";
    private static final String PROPERTY_REWARDED_AD_LAST_VERIFY_TIME = "RewardedAdLastVerifyTime";
    private static final String PROPERTY_REWARDED_AD_TOKEN = "RewardedAdToken";
    private static final String PROPERTY_REWARDED_AD_TOKEN_EXPIRE = "RewardedAdTokenExpire";
    private static final String PROPERTY_REWARDED_AD_TOKEN_CLAIMED = "RewardedAdTokenClaimed";
    private static final String PROPERTY_REWARDED_AD_CLAIM_COIN = "RewardedAdClaimCoin";
    private static final String PROPERTY_REWARDED_AD_RESULT = "RewardedAdResult";
    private static final String PROPERTY_REWARDED_AD_REVIEW = "RewardedAdClaimReview";

    private static final long TOKEN_EXPIRE_MS = 10L * 60L * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regRequestMessage(RequestMsgDef.REQ_REWARDED_AD_VERIFY.getId(), this, "OnReqRewardedAdVerify");
        kernel.regRequestMessage(RequestMsgDef.REQ_REWARDED_AD_CLAIM.getId(), this, "OnReqRewardedAdClaim");
        return true;
    }

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_DAY, ValueType.INT, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_DAILY_COUNT, ValueType.INT, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_LAST_VERIFY_TIME, ValueType.LONG, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_TOKEN, ValueType.STRING, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_TOKEN_EXPIRE, ValueType.LONG, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_TOKEN_CLAIMED, ValueType.BOOL, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_CLAIM_COIN, ValueType.LONG, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_RESULT, ValueType.STRING, false, false, true);
        kernel.declareProperty(script, PROPERTY_REWARDED_AD_REVIEW, ValueType.BOOL, false, false, true);
    }

    public void OnReqRewardedAdVerify(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        resp.put("valid", false);
        try {
            int uid = player.getInt(PLAYER_PROPERTY_UID);
            if (uid <= 0) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "invalid_player", false, "invalid_player");
                return;
            }

            long now = kernel.getServerTime();
            JSONObject request;
            try {
                request = data == null || data.length == 0 ? new JSONObject() : parseRequest(data);
                if (request == null || (request.containsKey("adResult")
                        && !(request.get("adResult") instanceof String))) {
                    throw new IllegalArgumentException("invalid adResult type");
                }
            } catch (Exception invalidRequest) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR, "invalid_request");
                return;
            }
            String adResult = request == null ? "" : trim(request.getString("adResult"));
            boolean requireResult = getBooleanConfig("rewardedAdRequireCompletionResult", false);
            if ((!adResult.isEmpty() && !"Finished".equals(adResult))
                    || (requireResult && adResult.isEmpty())) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                        "ad_not_completed", false, "ad_not_completed");
                return;
            }
            // Returning the pending credential makes verification retries safe after a lost ACK.
            if (player.getBool(PROPERTY_REWARDED_AD_REVIEW)) {
                resp.put("manualReview", true);
                resp.put("retryable", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "claim_requires_review", false, "claim_requires_review");
                return;
            }
            if (!player.getBool(PROPERTY_REWARDED_AD_TOKEN_CLAIMED)
                    && player.getLong(PROPERTY_REWARDED_AD_TOKEN_EXPIRE) > now
                    && !trim(player.getString(PROPERTY_REWARDED_AD_TOKEN)).isEmpty()) {
                resp.put("valid", true);
                resp.put("token", player.getString(PROPERTY_REWARDED_AD_TOKEN));
                resp.put("expiresAt", player.getLong(PROPERTY_REWARDED_AD_TOKEN_EXPIRE));
                respondSaved(kernel, player, reqid, resp, "ok");
                return;
            }
            resetDailyCountIfNeeded(player, now);

            int dailyLimit = getIntConfig("rewardedAdDailyLimit", 20);
            if (dailyLimit <= 0) {
                dailyLimit = 20; // A missing/zero limit must not turn the endpoint into unlimited currency.
            }
            int count = player.getInt(PROPERTY_REWARDED_AD_DAILY_COUNT);
            if (dailyLimit > 0 && count >= dailyLimit) {
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTimeInMillis(now);
                nextDay.add(Calendar.DAY_OF_MONTH, 1);
                nextDay.set(Calendar.HOUR_OF_DAY, 0);
                nextDay.set(Calendar.MINUTE, 0);
                nextDay.set(Calendar.SECOND, 0);
                nextDay.set(Calendar.MILLISECOND, 0);
                addRateLimit(resp, now, nextDay.getTimeInMillis(), "daily");
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                        "ok", false, "frequency_limit");
                return;
            }

            long minIntervalMs = Math.max(30000L, getLongConfig("rewardedAdMinIntervalMs", 30000L));
            long lastVerifyTime = player.getLong(PROPERTY_REWARDED_AD_LAST_VERIFY_TIME);
            if (lastVerifyTime > 0 && now - lastVerifyTime < minIntervalMs) {
                addRateLimit(resp, now, lastVerifyTime + minIntervalMs, "interval");
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                        "ok", false, "frequency_limit");
                return;
            }

            String token = newToken(uid);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN, token);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_EXPIRE, now + TOKEN_EXPIRE_MS);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, false);
            player.setProperty(PROPERTY_REWARDED_AD_CLAIM_COIN, 0L);
            player.setProperty(PROPERTY_REWARDED_AD_RESULT, adResult.isEmpty() ? "legacy_unknown" : adResult);
            player.setProperty(PROPERTY_REWARDED_AD_REVIEW, false);
            player.setProperty(PROPERTY_REWARDED_AD_LAST_VERIFY_TIME, now);
            player.setProperty(PROPERTY_REWARDED_AD_DAILY_COUNT, count + 1);

            resp.put("valid", true);
            resp.put("token", token);
            resp.put("expiresAt", now + TOKEN_EXPIRE_MS);
            respondSaved(kernel, player, reqid, resp, "ok");
        } catch (Exception e) {
            // Parser exception messages may include the original JSON/token.
            logger.error("rewarded ad verify error uid={} reqid={} exceptionType={}",
                    player.getInt(PLAYER_PROPERTY_UID), reqid, e.getClass().getSimpleName());
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", false, "server_error");
        }
    }

    public void OnReqRewardedAdClaim(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        resp.put("granted", false);
        resp.put("duplicate", false);
        boolean applyingClaim = false;
        try {
            JSONObject req;
            try {
                req = parseRequest(data);
            } catch (Exception invalidRequest) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR, "invalid_request");
                return;
            }
            String token = req == null ? "" : trim(req.getString("token"));
            Object rawCoin = req == null ? null : req.get("rewardCoin");
            boolean integerCoin = rawCoin instanceof Byte || rawCoin instanceof Short
                    || rawCoin instanceof Integer || rawCoin instanceof Long;
            long rewardCoin = integerCoin ? ((Number) rawCoin).longValue() : 0L;

            if (req == null || !(req.get("token") instanceof String) || token.isEmpty() || rewardCoin <= 0) {
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR, "invalid_request");
                return;
            }

            long now = kernel.getServerTime();
            String storedToken = trim(player.getString(PROPERTY_REWARDED_AD_TOKEN));
            long expire = player.getLong(PROPERTY_REWARDED_AD_TOKEN_EXPIRE);
            boolean claimed = player.getBool(PROPERTY_REWARDED_AD_TOKEN_CLAIMED);
            if (token.equals(storedToken) && player.getBool(PROPERTY_REWARDED_AD_REVIEW)) {
                resp.put("granted", false);
                resp.put("manualReview", true);
                resp.put("retryable", false);
                respondSaved(kernel, player, reqid, resp, "claim_requires_review", ServerCodeDef.CODE_FAILED);
                return;
            }
            if (token.equals(storedToken) && claimed
                    && player.getLong(PROPERTY_REWARDED_AD_CLAIM_COIN) == rewardCoin) {
                addClaimAudit(resp, player, token, rewardCoin);
                resp.put("granted", true);
                resp.put("duplicate", true);
                respondSaved(kernel, player, reqid, resp, "already_granted");
                return;
            }
            if (!token.equals(storedToken) || expire <= now || claimed) {
                resp.put("granted", false);
                resp.put("retryable", false);
                if (token.equals(storedToken) && claimed
                        && player.getLong(PROPERTY_REWARDED_AD_CLAIM_COIN) == 0L) {
                    // A rejected amount consumed this token. A previous archive may still
                    // be pending: do not acknowledge a terminal rejection before saving it.
                    respondSaved(kernel, player, reqid, resp, "token_invalid", ServerCodeDef.CODE_PROTECTED);
                    return;
                }
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED, "token_invalid");
                return;
            }

            if (!isRewardCoinReasonable(player, rewardCoin)) {
                player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, true);
                resp.put("granted", false);
                resp.put("retryable", false);
                respondSaved(kernel, player, reqid, resp, "reward_invalid");
                return;
            }

            long curMoney = player.getLong(PLAYER_MONEY);
            if (Long.MAX_VALUE - curMoney < rewardCoin) {
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED, "money_overflow");
                return;
            }

            applyingClaim = true;
            player.setProperty(PROPERTY_REWARDED_AD_REVIEW, true);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, true);
            player.setProperty(PLAYER_MONEY, curMoney + rewardCoin,
                    IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                    "rewarded ad claim +" + rewardCoin);
            player.setProperty(PROPERTY_REWARDED_AD_CLAIM_COIN, rewardCoin);
            player.setProperty(PROPERTY_REWARDED_AD_REVIEW, false);
            applyingClaim = false;

            addClaimAudit(resp, player, token, rewardCoin);
            resp.put("granted", true);
            resp.put("duplicate", false);
            respondSaved(kernel, player, reqid, resp, "ok");
        } catch (Exception e) {
            logger.error("rewarded ad claim error uid={} reqid={} exceptionType={}",
                    player.getInt(PLAYER_PROPERTY_UID), reqid, e.getClass().getSimpleName());
            resp.put("granted", false);
            if (applyingClaim) {
                // A property setter may have failed after consuming the token or changing money.
                // Keep the saved review marker: retries must not guess whether another credit is due.
                player.setProperty(PROPERTY_REWARDED_AD_REVIEW, true);
                resp.put("manualReview", true);
                resp.put("retryable", false);
                respondSaved(kernel, player, reqid, resp, "claim_requires_review", ServerCodeDef.CODE_FAILED);
                return;
            }
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED, "server_error");
        }
    }

    private static boolean isRewardCoinReasonable(IGameObject player, long rewardCoin) {
        long maxLevelIncome = Math.max(100L, player.getLong(PLAYER_MAX_LEVEL_INCOME));
        long baseMax = alignDownTo50((long) Math.ceil(maxLevelIncome * 1.5D));
        if (player.getBool(PLAYER_DOUBLE_COINS_PURCHASED)) {
            baseMax = saturatedMultiply(baseMax, 2L);
        }
        long x2Max = saturatedMultiply(baseMax, 2L);
        long allowedMax = (long) Math.ceil(x2Max * getDoubleConfig("rewardedAdRewardTolerance", 1.2D));
        return rewardCoin > 0 && rewardCoin <= allowedMax;
    }

    private static long alignDownTo50(long value) {
        if (value <= 0) {
            return 0;
        }
        return value / 50L * 50L;
    }

    private static long saturatedMultiply(long a, long b) {
        if (a <= 0 || b <= 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    private static void resetDailyCountIfNeeded(IGameObject player, long now) {
        int today = yyyymmdd(now);
        if (player.getInt(PROPERTY_REWARDED_AD_DAY) != today) {
            player.setProperty(PROPERTY_REWARDED_AD_DAY, today);
            player.setProperty(PROPERTY_REWARDED_AD_DAILY_COUNT, 0);
        }
    }

    private static String newToken(int uid) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("ad_reward_");
        sb.append(uid).append('_').append(System.currentTimeMillis()).append('_');
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static int yyyymmdd(long serverTimeMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(serverTimeMs);
        return cal.get(Calendar.YEAR) * 10000
                + (cal.get(Calendar.MONTH) + 1) * 100
                + cal.get(Calendar.DAY_OF_MONTH);
    }

    private static void respond(IKernel kernel, IGameObject player, int reqid,
                                JSONObject resp, ServerCodeDef code, String status) {
        resp.put("code", code.getId());
        resp.put("status", status);
        if (!resp.containsKey("retryable")) resp.put("retryable", false);
        if (!resp.containsKey("manualReview")) resp.put("manualReview", false);
        String token = trim(resp.getString("token"));
        String fingerprint = trim(resp.getString("rewardReference"));
        if (fingerprint.isEmpty()) {
            if (token.isEmpty()) token = trim(player.getString(PROPERTY_REWARDED_AD_TOKEN));
            fingerprint = token.isEmpty() ? "" : org.apache.commons.codec.digest.DigestUtils.sha256Hex(token);
        }
        if (!fingerprint.isEmpty()) fingerprint = fingerprint.substring(0, 8);
        logger.info("rewarded ad response uid={} reqid={} msgid={} code={} status={} valid={} granted={} "
                        + "duplicate={} retryable={} manualReview={} tokenFingerprint={} time={}",
                player.getInt(PLAYER_PROPERTY_UID), reqid, resp.containsKey("duplicate") ? 10022 : 10021,
                code.getId(), status, resp.getBooleanValue("valid"), resp.getBooleanValue("granted"),
                resp.getBooleanValue("duplicate"), resp.getBooleanValue("retryable"),
                resp.getBooleanValue("manualReview"), fingerprint, kernel.getServerTime());
        kernel.response(player, reqid, CustomMsg.String.newBuilder()
                .setValue(resp.toJSONString()).build().toByteArray());
    }

    private static void respondSaved(IKernel kernel, IGameObject player, int reqid,
                                     JSONObject response, String status) {
        respondSaved(kernel, player, reqid, response, status, ServerCodeDef.CODE_SUCCESS);
    }

    private static void respondSaved(IKernel kernel, IGameObject player, int reqid,
                                     JSONObject response, String status, ServerCodeDef acknowledgedCode) {
        JSONObject snapshot = JSON.parseObject(response.toJSONString());
        kernel.flushGooglePlayPlayer(player, saved -> {
            JSONObject reply = JSON.parseObject(snapshot.toJSONString());
            String savedToken = trim(snapshot.getString("token"));
            if (savedToken.isEmpty()) savedToken = trim(snapshot.getString("rewardReference"));
            else savedToken = org.apache.commons.codec.digest.DigestUtils.sha256Hex(savedToken);
            logger.info("rewarded ad archive ack uid={} reqid={} saved={} tokenFingerprint={}",
                    player.getInt(PLAYER_PROPERTY_UID), reqid, saved,
                    savedToken.isEmpty() ? "" : savedToken.substring(0, 8));
            reply.put("retryable", false);
            if (reply.containsKey("expiresAt")) {
                reply.put("expiresInMs", Math.max(0L, reply.getLongValue("expiresAt") - kernel.getServerTime()));
            }
            if (!Boolean.TRUE.equals(saved)) {
                reply.put("valid", false);
                reply.put("granted", false);
                boolean review = reply.getBooleanValue("manualReview");
                reply.put("retryable", !review);
                respond(kernel, player, reqid, reply, ServerCodeDef.CODE_FAILED,
                        review ? "claim_requires_review" : "player_save_pending");
            } else {
                respond(kernel, player, reqid, reply, acknowledgedCode, status);
            }
        });
    }

    private static void addClaimAudit(JSONObject response, IGameObject player, String token, long rewardCoin) {
        response.put("rewardReference", org.apache.commons.codec.digest.DigestUtils.sha256Hex(token));
        response.put("rewardCoin", rewardCoin);
        response.put("adResult", trim(player.getString(PROPERTY_REWARDED_AD_RESULT)));
        response.put("balanceProperty", PLAYER_MONEY);
        response.put("balance", player.getLong(PLAYER_MONEY));
    }

    private static JSONObject parseRequest(byte[] data) throws Exception {
        if (data == null || data.length == 0) throw new IllegalArgumentException("missing request");
        return JSON.parseObject(CustomMsg.String.parseFrom(data).getValue());
    }

    private static void addRateLimit(JSONObject response, long now, long nextAvailableAt, String limitType) {
        response.put("limitType", limitType);
        response.put("nextAvailableAt", nextAvailableAt);
        response.put("retryAfterMs", Math.max(0L, nextAvailableAt - now));
    }

    private static void respond(IKernel kernel, IGameObject player, int reqid,
                                JSONObject resp, ServerCodeDef code, String status,
                                boolean valid, String reason) {
        resp.put("valid", valid);
        if (reason != null && !reason.isEmpty()) {
            resp.put("reason", reason);
        }
        respond(kernel, player, reqid, resp, code, status);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static int getIntConfig(String key, int defaultValue) {
        Object configured = SystemConfigData.getConfig(key, (Object) defaultValue);
        if (configured instanceof Number) {
            return ((Number) configured).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(configured));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean getBooleanConfig(String key, boolean defaultValue) {
        Object configured = SystemConfigData.getConfig(key, (Object) defaultValue);
        return configured instanceof Number ? ((Number) configured).intValue() != 0
                : Boolean.parseBoolean(String.valueOf(configured));
    }

    private static long getLongConfig(String key, long defaultValue) {
        Object configured = SystemConfigData.getConfig(key, (Object) defaultValue);
        if (configured instanceof Number) {
            return ((Number) configured).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(configured));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static double getDoubleConfig(String key, double defaultValue) {
        Object configured = SystemConfigData.getConfig(key, (Object) defaultValue);
        if (configured instanceof Number) {
            return ((Number) configured).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(configured));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
