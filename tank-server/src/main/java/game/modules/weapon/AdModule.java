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
    }

    public void OnReqRewardedAdVerify(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        try {
            int uid = player.getInt(PLAYER_PROPERTY_UID);
            if (uid <= 0) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "invalid_player", false, "invalid_player");
                return;
            }

            long now = kernel.getServerTime();
            resetDailyCountIfNeeded(player, now);

            int dailyLimit = getIntConfig("rewardedAdDailyLimit", 20);
            int count = player.getInt(PROPERTY_REWARDED_AD_DAILY_COUNT);
            if (dailyLimit > 0 && count >= dailyLimit) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                        "ok", false, "frequency_limit");
                return;
            }

            long minIntervalMs = getLongConfig("rewardedAdMinIntervalMs", 0L);
            long lastVerifyTime = player.getLong(PROPERTY_REWARDED_AD_LAST_VERIFY_TIME);
            if (minIntervalMs > 0 && now - lastVerifyTime < minIntervalMs) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                        "ok", false, "frequency_limit");
                return;
            }

            String token = newToken(uid);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN, token);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_EXPIRE, now + TOKEN_EXPIRE_MS);
            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, false);
            player.setProperty(PROPERTY_REWARDED_AD_LAST_VERIFY_TIME, now);
            player.setProperty(PROPERTY_REWARDED_AD_DAILY_COUNT, count + 1);

            resp.put("valid", true);
            resp.put("token", token);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS, "ok");
        } catch (Exception e) {
            logger.error("OnReqRewardedAdVerify error", e);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", false, "server_error");
        }
    }

    public void OnReqRewardedAdClaim(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        try {
            CustomMsg.String msg = CustomMsg.String.parseFrom(data);
            JSONObject req = JSON.parseObject(msg.getValue());
            String token = req == null ? "" : trim(req.getString("token"));
            long rewardCoin = req == null ? 0L : req.getLongValue("rewardCoin");

            if (token.isEmpty() || rewardCoin <= 0) {
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR, "invalid_request");
                return;
            }

            long now = kernel.getServerTime();
            String storedToken = trim(player.getString(PROPERTY_REWARDED_AD_TOKEN));
            long expire = player.getLong(PROPERTY_REWARDED_AD_TOKEN_EXPIRE);
            boolean claimed = player.getBool(PROPERTY_REWARDED_AD_TOKEN_CLAIMED);
            if (!token.equals(storedToken) || expire <= now || claimed) {
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED, "token_invalid");
                return;
            }

            if (!isRewardCoinReasonable(player, rewardCoin)) {
                player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, true);
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS, "reward_invalid");
                return;
            }

            long curMoney = player.getLong(PLAYER_MONEY);
            if (Long.MAX_VALUE - curMoney < rewardCoin) {
                resp.put("granted", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED, "money_overflow");
                return;
            }

            player.setProperty(PROPERTY_REWARDED_AD_TOKEN_CLAIMED, true);
            player.setProperty(PLAYER_MONEY, curMoney + rewardCoin,
                    IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                    "rewarded ad claim +" + rewardCoin);

            resp.put("granted", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS, "ok");
        } catch (Exception e) {
            logger.error("OnReqRewardedAdClaim error", e);
            resp.put("granted", false);
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
        kernel.response(player, reqid, CustomMsg.String.newBuilder()
                .setValue(resp.toJSONString()).build().toByteArray());
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
