package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.IRecord;
import framework.mybatis.domain.GooglePlayOrder;
import game.custommsg.CustomMsg;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class SettlementTest implements PropertyKey {
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> temporary = new HashMap<>();
    private long now = 100000L;
    private boolean saveSucceeds = true;
    private boolean deferSave;
    private Consumer<Boolean> pendingSave;
    private JSONObject response;
    private String failPropertyOnce;
    private IGameObject player;
    private IKernel kernel;
    private LevelModule levels;
    private Object[] lastMoneyWrite;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        properties.put(PLAYER_PROPERTY_UID, 999001);
        player = (IGameObject) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGameObject.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("setProperty") || name.equals("setTempData")) {
                        if (name.equals("setProperty") && PLAYER_MONEY.equals(args[0])) {
                            lastMoneyWrite = args;
                        }
                        if (args[0].equals(failPropertyOnce)) {
                            failPropertyOnce = null;
                            throw new IllegalStateException("injected property failure");
                        }
                        (name.equals("setProperty") ? properties : temporary).put((String) args[0], args[1]);
                        return defaultValue(method.getReturnType());
                    }
                    if (name.equals("removeTempData")) {
                        temporary.remove(args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    if (name.startsWith("get") && args != null && args.length == 1 && args[0] instanceof String) {
                        Object value = (name.startsWith("getTemp") ? temporary : properties).get(args[0]);
                        if (method.getReturnType() == int.class) return value == null ? 0 : ((Number) value).intValue();
                        if (method.getReturnType() == long.class) return value == null ? 0L : ((Number) value).longValue();
                        if (method.getReturnType() == boolean.class) return Boolean.TRUE.equals(value);
                        return value;
                    }
                    return defaultValue(method.getReturnType());
                });
        kernel = (IKernel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKernel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getServerTime")) return now;
                    if (method.getName().equals("flushGooglePlayPlayer")) {
                        if (deferSave) pendingSave = (Consumer<Boolean>) args[1];
                        else ((Consumer<Boolean>) args[1]).accept(saveSucceeds);
                    }
                    if (method.getName().equals("response")) {
                        Object body = args[2];
                        response = body instanceof byte[]
                                ? JSON.parseObject(CustomMsg.String.parseFrom((byte[]) body).getValue())
                                : JSON.parseObject(body.toString());
                    }
                    return defaultValue(method.getReturnType());
                });
        levels = new LevelModule();
        Field config = LevelModule.class.getDeclaredField("m_mapLevel");
        config.setAccessible(true);
        Map<Integer, LevelModule.LevelData> map = (Map<Integer, LevelModule.LevelData>) config.get(levels);
        for (int id = 1; id <= 3; id++) {
            LevelModule.LevelData data = new LevelModule.LevelData();
            data.id = id;
            data.coin = 100;
            map.put(id, data);
        }
    }

    @Test
    public void strictSettlementRejectsMissingResolutionWithoutConsumingAttempt() throws Exception {
        String id = start(1);
        JSONObject request = new JSONObject();
        request.put("levelId", 1);
        request.put("coin", 0);
        request.put("isWin", true);
        request.put("attemptId", id);
        levels.OnReqLevelFinish(kernel, player, 10005, 1, bytes(request.toJSONString()));
        assertEquals(6, response.getIntValue("code"));
        assertEquals("resolution_required", response.getString("status"));
        request.put("resolution", null);
        levels.OnReqLevelFinish(kernel, player, 10005, 2, bytes(request.toJSONString()));
        assertEquals(6, response.getIntValue("code"));
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
        finish(id, 1, true, "Restart");
        assertEquals(0, response.getIntValue("code"));
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
    }

    @Test
    public void countProgressFalseOverridesLegacyWinAndMalformedFlagCannotSettle() throws Exception {
        levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":1}"));
        levels.OnReqLevelFinish(kernel, player, 10005, 2,
                bytes("{\"levelId\":1,\"coin\":0,\"isWin\":true,\"countProgress\":\"false\"}"));
        assertEquals(6, response.getIntValue("code"));
        levels.OnReqLevelFinish(kernel, player, 10005, 3,
                bytes("{\"levelId\":1,\"coin\":0,\"isWin\":true,\"countProgress\":false}"));
        assertEquals(0, response.getIntValue("code"));
        assertFalse(response.getBooleanValue("isWin"));
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
    }

    @Test
    public void nextStartDelayIsRecomputedAfterSlowArchiveAck() throws Exception {
        String id = start(1);
        deferSave = true;
        finish(id, 1, false, "Restart");
        now += 1200;
        pendingSave.accept(true);
        assertEquals(0L, response.getLongValue("retryAfterMs"));
        deferSave = false;
        start(1);
        assertEquals(0, response.getIntValue("code"));
    }

    @Test
    public void progressRepairInvalidatesOldAttemptAndOldReceiptReportsCurrentProgress() throws Exception {
        String id = start(1);
        properties.put(PLAYER_PROGRESS_REVISION, 2);
        finish(id, 1, true, "Won");
        assertEquals("progress_revision_changed", response.getString("status"));
        assertFalse(properties.containsKey(PLAYER_MONEY));
        String fresh = start(1);
        assertNotEquals(id, fresh);
        finish(fresh, 1, true, "Won");
        properties.put(PLAYER_PROGRESS_REVISION, 3);
        properties.put(PLAYER_LEVELS_COMPLETED, 0);
        finish(fresh, 1, true, "Won");
        assertTrue(response.getBooleanValue("duplicate"));
        assertEquals(3, response.getIntValue("progressRevision"));
        assertEquals(0, response.getIntValue("levelsCompleted"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void loginInitializesButDoesNotResetProgressRevision() {
        levels.OnPlayerOnLine(kernel, player);
        assertEquals(1, properties.get(PLAYER_PROGRESS_REVISION));
        properties.put(PLAYER_PROGRESS_REVISION, 5);
        levels.OnPlayerOffLine(kernel, player);
        levels.OnPlayerOnLine(kernel, player);
        assertEquals(5, properties.get(PLAYER_PROGRESS_REVISION));
    }

    @Test
    public void queryingWeaponsMustNotResetPlayerBalance() {
        properties.put(PLAYER_MONEY, 5770L);
        WeaponModule weapons = new WeaponModule();
        weapons.OnReqWeaponGetData(kernel, player, 10000, 1, null);
        weapons.OnReqWeaponGetData(kernel, player, 10000, 2, null);
        assertEquals(5770L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void iapUsesPlayerMoneyAndKeepsLegacyGoldUntouchedIncludingOverflow() throws Exception {
        ShopModule shop = new ShopModule();
        try {
            Method grant = ShopModule.class.getDeclaredMethod("grantMoney", IGameObject.class, long.class);
            grant.setAccessible(true);
            properties.put(PLAYER_MONEY, 5770L);
            properties.put(PLAYER_PROPERTY_GOLD, 123L);
            assertEquals("GRANTED", grant.invoke(shop, player, 600000L).toString());
            assertEquals(605770L, properties.get(PLAYER_MONEY));
            assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
            assertEquals(IKernel.PlayerLogType.PROP_CHANGE.ordinal(), lastMoneyWrite[2]);
            assertEquals("google iap reward amount=600000", lastMoneyWrite[3]);
            assertEquals("GRANTED", grant.invoke(shop, player, 1000L).toString());
            assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
            assertEquals(606770L, properties.get(PLAYER_MONEY));
            properties.put(PLAYER_MONEY, Long.MAX_VALUE);
            lastMoneyWrite = null;
            assertEquals("NO_EFFECT", grant.invoke(shop, player, 1L).toString());
            assertEquals(Long.MAX_VALUE, properties.get(PLAYER_MONEY));
            assertNull(lastMoneyWrite);
            assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
        } finally {
            shop.onDestroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void iapCreditCanBeSpentOnWeaponThenSettledWithoutBalanceJump() throws Exception {
        ShopModule shop = new ShopModule();
        try {
            properties.put(PLAYER_MONEY, 5770L);
            properties.put(PLAYER_PROPERTY_GOLD, 123L);
            Method grant = ShopModule.class.getDeclaredMethod("grantMoney", IGameObject.class, long.class);
            grant.setAccessible(true);
            assertEquals("GRANTED", grant.invoke(shop, player, 600000L).toString());
            WeaponModule weapons = new WeaponModule();
            Field configs = WeaponModule.class.getDeclaredField("m_mapWeapon");
            configs.setAccessible(true);
            WeaponModule.WeaponUpgradesData config = new WeaponModule.WeaponUpgradesData();
            config.weaponTypes = 0;
            config.price = new int[]{100, 200, 300, 400, 500, 600};
            ((Map<Integer, WeaponModule.WeaponUpgradesData>) configs.get(weapons)).put(0, config);
            weapons.OnReqWeaponUpgrade(kernel, player, 10001, 127, bytes("{\"weaponType\":0}"));
            assertEquals(605570L, properties.get(PLAYER_MONEY));
            String attempt = start(1);
            finish(attempt, 1, true, "Won");
            assertEquals(0, response.getIntValue("code"));
            assertEquals(605570L, response.getLongValue("balanceBefore"));
            assertEquals(605670L, response.getLongValue("balanceAfter"));
            assertEquals(605670L, properties.get(PLAYER_MONEY));
            assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
        } finally {
            shop.onDestroy();
        }
    }

    @Test
    public void fulfilledResponseReportsActualWalletAndDuplicateTerminalState() throws Exception {
        ShopModule shop = new ShopModule();
        try {
            Class<?> productType = Class.forName(ShopModule.class.getName() + "$GooglePlayProduct");
            Constructor<?> constructor = productType.getDeclaredConstructor(String.class, int.class,
                    boolean.class, int.class, long.class, int.class, int.class);
            constructor.setAccessible(true);
            Method finish = ShopModule.class.getDeclaredMethod("finishSuccess", IKernel.class, IGameObject.class,
                    int.class, JSONObject.class, productType, String.class, boolean.class, boolean.class,
                    String.class, String.class);
            finish.setAccessible(true);
            Object product = constructor.newInstance("google_gold_12", 12, true, 2, 600000L, 0, 0);
            properties.put(PLAYER_MONEY, 605770L);
            properties.put(PLAYER_PROPERTY_GOLD, 123L);
            finish.invoke(shop, kernel, player, 123, new JSONObject(), product, "GPA.TEST", false, true, "", "");
            assertEquals(0, response.getIntValue("code"));
            assertEquals("fulfilled", response.getString("status"));
            assertEquals("PlayerMoney", response.getString("balanceProperty"));
            assertEquals(605770L, response.getLongValue("gold"));
            assertEquals(600000L, response.getLongValue("rewardGold"));
            assertTrue(response.getBooleanValue("consumePending"));
            finish.invoke(shop, kernel, player, 124, new JSONObject(), product, "GPA.TEST", true, false, "", "");
            assertEquals("already_fulfilled", response.getString("status"));
            assertTrue(response.getBooleanValue("duplicate"));
            assertEquals(605770L, properties.get(PLAYER_MONEY));
            Object legacy = constructor.newInstance("google_gold_12", 12, true, 1, 600000L, 0, 0);
            finish.invoke(shop, kernel, player, 125, new JSONObject(), legacy, "GPA.OLD", true, false, "", "");
            assertEquals("PlayerMoney", response.getString("balanceProperty"));
            assertEquals(605770L, response.getLongValue("gold"));
            assertEquals(1, response.getIntValue("rewardVersion"));
            assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
        } finally {
            shop.onDestroy();
        }
    }

    @Test
    public void iapListenerFailuresKeepGrantedNoEffectAndUnknownSemantics() throws Exception {
        ShopModule shop = new ShopModule();
        try {
            Method grant = ShopModule.class.getDeclaredMethod("grantMoney", IGameObject.class, long.class);
            grant.setAccessible(true);
            for (long delta : new long[]{0L, 600000L, 1L}) {
                properties.put(PLAYER_MONEY, 5770L);
                properties.put(PLAYER_PROPERTY_GOLD, 123L);
                IGameObject failing = (IGameObject) Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[]{IGameObject.class}, (proxy, method, args) -> {
                            if (method.getName().equals("setProperty")) {
                                assertEquals(PLAYER_MONEY, args[0]);
                                properties.put(PLAYER_MONEY, 5770L + delta);
                                throw new IllegalStateException("injected money listener failure");
                            }
                            return method.invoke(player, args);
                        });
                assertEquals(delta == 0 ? "NO_EFFECT" : delta == 600000 ? "GRANTED" : "UNKNOWN",
                        grant.invoke(shop, failing, 600000L).toString());
                assertEquals(5770L + delta, properties.get(PLAYER_MONEY));
                assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
            }
        } finally {
            shop.onDestroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replayOfFulfilledV1AndV2OrdersNeverCreditsEitherWallet() throws Exception {
        String oldPackage = System.getProperty("googlePlayPackageName");
        String oldSecret = System.getProperty("googlePlayAccountIdSecret");
        System.setProperty("googlePlayPackageName", "example.tank.test");
        System.setProperty("googlePlayAccountIdSecret", "test-only-secret-32-characters-long");
        ShopModule shop = new ShopModule();
        try {
            Class<?> productType = Class.forName(ShopModule.class.getName() + "$GooglePlayProduct");
            Constructor<?> constructor = productType.getDeclaredConstructor(String.class, int.class,
                    boolean.class, int.class, long.class, int.class, int.class);
            constructor.setAccessible(true);
            Method save = ShopModule.class.getDeclaredMethod("saveCompletedLocalOrder", IRecord.class,
                    String.class, productType, String.class, long.class, int.class, int.class);
            save.setAccessible(true);
            Method hash = ShopModule.class.getDeclaredMethod("sha256Hex", String.class);
            hash.setAccessible(true);
            IKernel recovery = (IKernel) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IKernel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getPlayer")) return player;
                        if (method.getName().equals("declareRecord")) return properties.get("GooglePlayOrderRec");
                        if (method.getName().equals("executeGooglePlayToStore")) {
                            assertEquals("completeFromPlayerRecord", args[1]);
                            GooglePlayOrder order = (GooglePlayOrder) ((List<?>) args[2]).get(0);
                            ((Consumer<String>) args[3]).accept(JSON.toJSONString(order));
                            return null;
                        }
                        return method.invoke(kernel, args);
                    });
            properties.put(PLAYER_MONEY, 605770L);
            properties.put(PLAYER_PROPERTY_GOLD, 123L);
            for (int version : new int[]{1, 2}) {
                framework.game.Record rec = new framework.game.Record("GooglePlayOrderRec", 16, 200,
                        false, false, true);
                properties.put("GooglePlayOrderRec", rec);
                shop.OnPlayerClassCreate(recovery, "Player");
                Object product = constructor.newInstance("google_gold_12", 12, true, version, 600000L, 0, 0);
                String token = "test-fulfilled-v" + version;
                assertEquals(0, save.invoke(shop, rec, hash.invoke(null, token), product, "GPA.TEST", now, 4, 1));
                JSONObject request = new JSONObject();
                request.put("productId", "google_gold_12");
                request.put("purchaseToken", token);
                lastMoneyWrite = null;
                response = null;
                shop.OnGooglePlayBuyGold(recovery, player, 10018, 126,
                        CustomMsg.String.newBuilder().setValue(request.toJSONString()).build().toByteArray());
                assertNotNull(response);
                assertEquals(0, response.getIntValue("code"));
                assertEquals("already_fulfilled", response.getString("status"));
                assertTrue(response.getBooleanValue("duplicate"));
                assertFalse(response.getBooleanValue("consumePending"));
                assertEquals("PlayerMoney", response.getString("balanceProperty"));
                assertEquals(605770L, response.getLongValue("gold"));
                assertEquals(version, rec.getInt(0, 7));
                assertEquals(version, response.getIntValue("rewardVersion"));
                assertEquals(605770L, properties.get(PLAYER_MONEY));
                assertEquals(123L, properties.get(PLAYER_PROPERTY_GOLD));
                assertNull(lastMoneyWrite);
            }
        } finally {
            shop.onDestroy();
            if (oldPackage == null) System.clearProperty("googlePlayPackageName");
            else System.setProperty("googlePlayPackageName", oldPackage);
            if (oldSecret == null) System.clearProperty("googlePlayAccountIdSecret");
            else System.setProperty("googlePlayAccountIdSecret", oldSecret);
        }
    }

    @Test
    public void processingResponseIsRetryableAndDoesNotGrantMoney() throws Exception {
        ShopModule shop = new ShopModule();
        try {
            Method respond = ShopModule.class.getDeclaredMethod("respond", IKernel.class, IGameObject.class,
                    int.class, JSONObject.class, game.custommsg.ServerCodeDef.class, String.class,
                    String.class, String.class);
            respond.setAccessible(true);
            properties.put(PLAYER_MONEY, 5770L);
            respond.invoke(shop, kernel, player, 123, new JSONObject(),
                    game.custommsg.ServerCodeDef.CODE_PROTECTED, "processing", "", "");
            assertEquals(23, response.getIntValue("code"));
            assertTrue(response.getBooleanValue("retryable"));
            assertEquals("processing", response.getString("status"));
            assertEquals(5770L, properties.get(PLAYER_MONEY));
        } finally {
            shop.onDestroy();
        }
    }

    @Test
    public void successIsNotAcknowledgedBeforeArchiveCommit() throws Exception {
        deferSave = true;
        start(1);
        assertNull(response);
        pendingSave.accept(true);
        String id = response.getString("attemptId");
        response = null;
        finish(id, 1, true, "Won");
        assertNull(response);
        pendingSave.accept(true);
        assertEquals(0, response.getIntValue("code"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void expiredAttemptCannotGrantMoney() throws Exception {
        String id = start(1);
        now += LevelAttemptState.MAX_AGE_MS;
        finish(id, 1, true, "Won");
        assertEquals("attempt_expired", response.getString("status"));
        assertFalse(properties.containsKey(PLAYER_MONEY));
    }

    @Test
    public void reconnectAndLostAckOnlyAwardOnce() throws Exception {
        String id = start(1);
        levels.OnPlayerOffLine(kernel, player);
        levels.OnPlayerOnLine(kernel, player);
        finish(id, 1, true, "Won");
        assertEquals(0, response.getIntValue("code"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
        assertEquals(1, properties.get(PLAYER_LEVELS_COMPLETED));
        finish(id, 1, true, "Won");
        assertEquals(0, response.getIntValue("code"));
        assertTrue(response.getBooleanValue("duplicate"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void settlementAuditKeepsOriginalDeltaAcrossDelayedAckAndReplay() throws Exception {
        properties.put(PLAYER_MONEY, 50L);
        String id = start(1);
        deferSave = true;
        finish(id, 1, true, "Won");
        properties.put(PLAYER_MONEY, 130L); // A later operation must not rewrite this receipt's delta.
        pendingSave.accept(true);
        assertEquals(0, response.getIntValue("levelsCompletedBefore"));
        assertEquals(1, response.getIntValue("levelsCompletedAfter"));
        assertEquals(50L, response.getLongValue("balanceBefore"));
        assertEquals(150L, response.getLongValue("balanceAfter"));
        deferSave = false;
        finish(id, 1, true, "Won");
        assertTrue(response.getBooleanValue("duplicate"));
        assertEquals(50L, response.getLongValue("balanceBefore"));
        assertEquals(150L, response.getLongValue("balanceAfter"));
        assertEquals(130L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void replayOfOldReceiptDoesNotConsumeNextLevel() throws Exception {
        String first = start(1);
        finish(first, 1, true, "Won");
        now += 1001;
        String second = start(2);
        finish(first, 1, true, "Won");
        assertTrue(response.getBooleanValue("duplicate"));
        finish(second, 2, true, "Won");
        assertEquals(2, properties.get(PLAYER_LEVELS_COMPLETED));
        assertEquals(200L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void failureAndRestartOverrideIncorrectWinFlag() throws Exception {
        String id = start(1);
        finish(id, 1, true, "Failed");
        assertEquals(0, response.getIntValue("code"));
        assertFalse(response.getBooleanValue("isWin"));
        assertTrue(response.getBooleanValue("reportedIsWin"));
        assertEquals(0, response.getIntValue("levelsCompletedBefore"));
        assertEquals(0, response.getIntValue("levelsCompletedAfter"));
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
        now += 1001;
        finish(start(1), 1, true, "Restart");
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
    }

    @Test
    public void badRequestCannotAwardOrConsumeAttempt() throws Exception {
        String id = start(1);
        finish("unknown", 1, true, "Won");
        assertEquals(22, response.getIntValue("code"));
        finish(null, 1, true, "Won");
        assertEquals(6, response.getIntValue("code"));
        assertEquals("attempt_id_required", response.getString("status"));
        levels.OnReqLevelFinish(kernel, player, 10005, 1, bytes("{\"levelId\":1,\"coin\":100}"));
        assertEquals(6, response.getIntValue("code"));
        assertFalse(properties.containsKey(PLAYER_MONEY));
        finish(id, 1, true, "Won");
        assertEquals(0, response.getIntValue("code"));
    }

    @Test
    public void saveFailureRetriesReceiptWithoutAddingMoney() throws Exception {
        String id = start(1);
        saveSucceeds = false;
        finish(id, 1, true, "Won");
        assertEquals(8, response.getIntValue("code"));
        assertTrue(response.getBooleanValue("retryable"));
        saveSucceeds = true;
        finish(id, 1, true, "Won");
        assertEquals(0, response.getIntValue("code"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void pendingStartAndSavedStateSurviveReconstruction() throws Exception {
        String id = start(1);
        assertEquals(id, start(1));
        assertTrue(response.getBooleanValue("resumed"));
        String saved = (String) properties.get(LevelAttemptState.PROPERTY);
        assertEquals(id, new LevelAttemptState(saved).active().getString("attemptId"));
        temporary.clear();
        finish(id, 1, true, "Won");
        LevelAttemptState restored = new LevelAttemptState((String) properties.get(LevelAttemptState.PROPERTY));
        assertNull(restored.active());
        assertEquals(100, restored.receipt(id).getIntValue("awardGold"));
        now += 100;
        start(2);
        assertEquals(35, response.getIntValue("code"));
        assertEquals("level_restart_cooldown", response.getString("status"));
        assertEquals(900, response.getIntValue("retryAfterMs"));
    }

    @Test
    public void legacyProtocolStillWorks() throws Exception {
        levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":1}"));
        levels.OnReqLevelFinish(kernel, player, 10005, 1,
                bytes("{\"levelId\":1,\"coin\":100,\"isWin\":true}"));
        assertEquals(0, response.getIntValue("code"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void adSkipRejectedAndClaimRetryDoesNotPayTwice() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Skipped\"}"));
        assertFalse(response.getBooleanValue("valid"));
        assertFalse(properties.containsKey("RewardedAdToken"));
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        assertTrue(response.getBooleanValue("valid"));
        String token = response.getString("token");
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals(token, response.getString("token"));
        JSONObject claim = new JSONObject();
        claim.put("token", token);
        claim.put("rewardCoin", 100);
        saveSucceeds = false;
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 1, bytes(claim.toJSONString()));
        assertEquals(8, response.getIntValue("code"));
        saveSucceeds = true;
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 1, bytes(claim.toJSONString()));
        assertTrue(response.getBooleanValue("duplicate"));
        assertTrue(response.getBooleanValue("granted"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{}"));
        assertFalse(response.getBooleanValue("valid"));
        assertEquals("frequency_limit", response.getString("reason"));
    }

    @Test
    public void cancellationSuppressesAllIncomeEvenWithPositiveReportedMetrics() throws Exception {
        Field targets = LevelModule.class.getDeclaredField("m_LittleTargetsModule");
        targets.setAccessible(true);
        targets.set(levels, new LittleTargetsModule() {
            @Override public boolean onLevelFinish(IKernel k, IGameObject p, int id, long delta) {
                throw new AssertionError("cancelled target must not settle");
            }
        });
        Field achievements = LevelModule.class.getDeclaredField("m_AchievementModule");
        achievements.setAccessible(true);
        achievements.set(levels, new AchievementModule() {
            @Override AchievementData getAchievementData(int id) {
                throw new AssertionError("cancelled achievements must not settle");
            }
        });
        properties.put(PLAYER_MONEY, 75L);
        for (String resolution : new String[]{"Restart", "Aborted"}) {
            String id = start(1);
            JSONObject request = finishRequest(id, resolution, false);
            request.put("countProgress", false);
            request.put("achievementCounts", JSON.parseObject("{\"0\":5}"));
            request.put("littleTargetId", 999);
            request.put("littleTargetDelta", 99);
            submit(request);
            assertEquals(0, response.getIntValue("code"));
            assertEquals(0L, response.getLongValue("awardGold"));
            assertTrue(response.getBooleanValue("earningsSuppressed"));
            assertEquals(75L, properties.get(PLAYER_MONEY));
            now += 1001;
        }
    }

    @Test
    public void wonConflictsAreRejectedWithoutConsumingAttempt() throws Exception {
        String id = start(1);
        submit(finishRequest(id, "Won", false));
        assertEquals("resolution_mismatch", response.getString("status"));
        JSONObject request = finishRequest(id, "Won", true);
        request.put("countProgress", false);
        submit(request);
        assertEquals("count_progress_mismatch", response.getString("status"));
        request.put("countProgress", true);
        request.put("futureExtension", new JSONObject());
        submit(request);
        assertEquals(0, response.getIntValue("code"));
        assertEquals(1, properties.get(PLAYER_LEVELS_COMPLETED));
    }

    @Test
    public void startRejectsUnknownVersionsAndUnsupportedModes() throws Exception {
        for (String version : new String[]{"0", "3", "2.0", "\"2\"", "null"}) {
            levels.OnReqLevelStart(kernel, player, 10006, 1,
                    bytes("{\"levelId\":1,\"settlementVersion\":" + version + "}"));
            assertEquals("unsupported_settlement_version", response.getString("status"));
        }
        for (String mode : new String[]{"ArenaLevel", "CustomLevel", "EditorLevel"}) {
            levels.OnReqLevelStart(kernel, player, 10006, 1,
                    bytes("{\"levelId\":1,\"settlementVersion\":2,\"gameplayMode\":\"" + mode + "\"}"));
            assertEquals("unsupported_gameplay_mode", response.getString("status"));
        }
        levels.OnReqLevelStart(kernel, player, 10006, 1,
                bytes("{\"levelId\":1,\"gameplayMode\":\"RegularLevel\"}"));
        assertEquals("gameplay_mode_mismatch", response.getString("status"));
        levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":-4294967295}"));
        assertEquals("invalid_level_id", response.getString("status"));
        assertFalse(properties.containsKey(LevelAttemptState.PROPERTY));
        String id = start(1);
        JSONObject request = finishRequest(id, "Won", true);
        request.put("levelId", -4294967295L);
        submit(request);
        assertEquals("invalid_level_id", response.getString("status"));
        assertFalse(properties.containsKey(PLAYER_MONEY));
    }

    @Test
    public void pendingAttemptUpgradesWithoutRerollAndRequiresExplicitAbandonment() throws Exception {
        levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":1}"));
        String id = response.getString("attemptId");
        assertEquals(1, response.getIntValue("settlementVersion"));
        assertEquals(id, start(1));
        assertEquals(2, response.getIntValue("settlementVersion"));
        assertEquals(now + LevelAttemptState.MAX_AGE_MS, response.getLongValue("attemptExpiresAt"));
        start(2);
        assertEquals("unfinished_level", response.getString("status"));
        assertEquals(id, response.getString("attemptId"));
        submit(finishRequest(id, "Aborted", false));
        now += 1001;
        assertNotEquals(id, start(1));
    }

    @Test
    public void mandatoryV2SwitchRejectsLegacyAndCanBeRolledBack() throws Exception {
        String previous = System.getProperty("levelSettlementRequireV2");
        try {
            levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":1}"));
            System.setProperty("levelSettlementRequireV2", "true");
            levels.OnReqLevelStart(kernel, player, 10006, 1, bytes("{\"levelId\":1}"));
            assertEquals("settlement_upgrade_required", response.getString("status"));
            levels.OnReqLevelFinish(kernel, player, 10005, 1, bytes("{\"levelId\":1,\"coin\":0,\"isWin\":true}"));
            assertEquals("settlement_upgrade_required", response.getString("status"));
            String id = start(1);
            assertNotNull(id);
            System.setProperty("levelSettlementRequireV2", "false");
            finish(null, 1, true, "Won");
            assertEquals("attempt_id_required", response.getString("status"));
            finish(id, 1, true, "Won");
            assertEquals(0, response.getIntValue("code"));
        } finally {
            if (previous == null) System.clearProperty("levelSettlementRequireV2");
            else System.setProperty("levelSettlementRequireV2", previous);
        }
    }

    @Test
    public void strictMetricsAndTargetMustMatchServerStart() throws Exception {
        Field targets = LevelModule.class.getDeclaredField("m_LittleTargetsModule");
        targets.setAccessible(true);
        targets.set(levels, new LittleTargetsModule() {
            @Override public LittleTargetData onLevelStart(IKernel k, IGameObject p) {
                LittleTargetData target = new LittleTargetData(); target.id = 0; target.targetValues = 30;
                return target;
            }
            @Override public boolean onLevelFinish(IKernel k, IGameObject p, int id, long delta) {
                assertEquals(0, id); return false;
            }
        });
        String id = start(1);
        assertTrue(response.getBooleanValue("littleTargetAssigned"));
        JSONObject request = finishRequest(id, "Failed", false);
        request.put("coin", 0.5D);
        submit(request);
        assertEquals("invalid_metrics", response.getString("status"));
        request.put("coin", 50);
        request.put("achievementCounts", JSON.parseObject("{\"0\":-1}"));
        submit(request);
        assertEquals("invalid_metrics", response.getString("status"));
        request.put("achievementCounts", new JSONObject());
        request.put("littleTargetId", 1); request.put("littleTargetDelta", 2);
        submit(request);
        assertEquals("little_target_mismatch", response.getString("status"));
        request.put("littleTargetId", 0);
        submit(request);
        assertEquals(0, response.getIntValue("code"));
        assertEquals(50L, properties.get(PLAYER_MONEY));
        assertFalse(properties.containsKey(PLAYER_LEVELS_COMPLETED));
    }

    @Test
    public void adContractStrictResultAndCredentialExpiry() {
        String previous = System.getProperty("rewardedAdRequireCompletionResult");
        try {
            System.setProperty("rewardedAdRequireCompletionResult", "true");
            AdModule ads = new AdModule();
            for (String request : new String[]{"{}", "{\"adResult\":\"foo\"}", "{\"adResult\":\"Failed\"}"}) {
                ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes(request));
                assertEquals(23, response.getIntValue("code"));
                assertEquals("ad_not_completed", response.getString("status"));
                assertFalse(response.containsKey("token"));
            }
            ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
            String token = response.getString("token");
            assertEquals(now + 600000, response.getLongValue("expiresAt"));
            assertEquals(600000, response.getLongValue("expiresInMs"));
            ads.OnReqRewardedAdClaim(kernel, player, 10022, 2,
                    bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100.5}"));
            assertEquals(6, response.getIntValue("code"));
            assertEquals("invalid_request", response.getString("status"));
            assertFalse(properties.get("RewardedAdTokenClaimed") == Boolean.TRUE);
            now += 600000;
            ads.OnReqRewardedAdClaim(kernel, player, 10022, 2,
                    bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}"));
            assertEquals(23, response.getIntValue("code"));
            assertEquals("token_invalid", response.getString("status"));
        } finally {
            if (previous == null) System.clearProperty("rewardedAdRequireCompletionResult");
            else System.setProperty("rewardedAdRequireCompletionResult", previous);
        }
    }

    @Test
    public void partialAdClaimNeedsReviewAndRetryCannotPayAgain() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        failPropertyOnce = PLAYER_MONEY;
        byte[] claim = bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}");
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2, claim);
        assertEquals(8, response.getIntValue("code"));
        assertEquals("claim_requires_review", response.getString("status"));
        assertTrue(response.getBooleanValue("manualReview"));
        saveSucceeds = false;
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 3, claim);
        assertEquals("claim_requires_review", response.getString("status"));
        assertFalse(response.getBooleanValue("retryable"));
        assertFalse(properties.containsKey(PLAYER_MONEY));
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 4, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals("claim_requires_review", response.getString("status"));
    }

    @Test
    public void p3VerifyRetryBypassesIntervalAndDoesNotUseAnotherQuota() {
        AdModule ads = new AdModule();
        saveSucceeds = false;
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 101, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals("player_save_pending", response.getString("status"));
        assertTrue(response.getBooleanValue("retryable"));
        assertFalse(response.getBooleanValue("valid"));
        String token = response.getString("token");
        now += 2000;
        saveSucceeds = true;
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 102, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals("ok", response.getString("status"));
        assertEquals(token, response.getString("token"));
        assertEquals(1, properties.get("RewardedAdDailyCount"));
        assertEquals(598000, response.getLongValue("expiresInMs"));
        assertFalse(response.getBooleanValue("retryable"));
    }

    @Test
    public void p3IntervalAndDailyLimitAreDistinguishableWithoutChangingLegacyReason() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}"));
        now += 2000;
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 3, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals(0, response.getIntValue("code"));
        assertEquals("ok", response.getString("status"));
        assertEquals("frequency_limit", response.getString("reason"));
        assertEquals("interval", response.getString("limitType"));
        assertEquals(28000, response.getLongValue("retryAfterMs"));
        assertEquals(now + 28000, response.getLongValue("nextAvailableAt"));
        assertFalse(response.getBooleanValue("valid"));
        assertFalse(response.getBooleanValue("retryable"));
        properties.put("RewardedAdDailyCount", 20);
        now += 30000;
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 4, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals("daily", response.getString("limitType"));
        assertTrue(response.getLongValue("retryAfterMs") > 0);
        assertEquals(20, properties.get("RewardedAdDailyCount"));
    }

    @Test
    public void p3LostClaimAckCanBeRecoveredAfterExpiryButNotAfterTokenReplacement() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        byte[] claim = bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}");
        saveSucceeds = false;
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2, claim);
        assertEquals("player_save_pending", response.getString("status"));
        assertFalse(response.getBooleanValue("granted"));
        assertTrue(response.getBooleanValue("retryable"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
        now += 600001;
        saveSucceeds = true;
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 3, claim);
        assertEquals("already_granted", response.getString("status"));
        assertTrue(response.getBooleanValue("granted"));
        assertTrue(response.getBooleanValue("duplicate"));
        assertEquals(100, response.getLongValue("rewardCoin"));
        assertEquals(100, response.getLongValue("balance"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 4, bytes("{\"adResult\":\"Finished\"}"));
        assertNotEquals(token, response.getString("token"));
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 5, claim);
        assertEquals("token_invalid", response.getString("status"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void p3DifferentAmountCannotInvalidateTheOriginalSuccessfulReceipt() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        byte[] claim = bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}");
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2, claim);
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 3,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":200}"));
        assertEquals("token_invalid", response.getString("status"));
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 4, claim);
        assertEquals("already_granted", response.getString("status"));
        assertEquals(100L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void p3MalformedRequestsAreTerminalAndDoNotConsumeQuotaOrToken() {
        AdModule ads = new AdModule();
        for (String request : new String[]{"{", "[]", "null", "{\"adResult\":null}", "{\"adResult\":123}"}) {
            ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes(request));
            assertEquals("invalid_request", response.getString("status"));
            assertEquals(6, response.getIntValue("code"));
            assertFalse(response.getBooleanValue("retryable"));
        }
        assertFalse(properties.containsKey("RewardedAdToken"));
        for (byte[] request : new byte[][]{null, new byte[]{(byte) 255}, bytes("{"), bytes("null"),
                bytes("{\"token\":123,\"rewardCoin\":100}"), bytes("{\"token\":\"test\",\"rewardCoin\":\"100\"}")}) {
            ads.OnReqRewardedAdClaim(kernel, player, 10022, 2, request);
            assertEquals(6, response.getIntValue("code"));
            assertFalse(response.getBooleanValue("granted"));
            assertFalse(response.getBooleanValue("retryable"));
            assertFalse(response.getBooleanValue("manualReview"));
        }
    }

    @Test
    public void p3DoubleCoinsAndChestMultiplierFitServerCapAndOversizeConsumesToken() {
        AdModule ads = new AdModule();
        properties.put(PLAYER_DOUBLE_COINS_PURCHASED, true);
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":600}"));
        assertTrue(response.getBooleanValue("granted"));
        assertEquals(600L, properties.get(PLAYER_MONEY));
        now += 30000;
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 3, bytes("{\"adResult\":\"Finished\"}"));
        token = response.getString("token");
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 4,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":721}"));
        assertEquals("reward_invalid", response.getString("status"));
        assertFalse(response.getBooleanValue("granted"));
        assertTrue(player.getBool("RewardedAdTokenClaimed"));
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 5,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}"));
        assertEquals("token_invalid", response.getString("status"));
        assertEquals(600L, properties.get(PLAYER_MONEY));
    }

    @Test
    public void p3OverflowIsTerminalWithoutChangingWalletOrSettingServerReview() {
        AdModule ads = new AdModule();
        ads.OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        String token = response.getString("token");
        properties.put(PLAYER_MONEY, Long.MAX_VALUE);
        ads.OnReqRewardedAdClaim(kernel, player, 10022, 2,
                bytes("{\"token\":\"" + token + "\",\"rewardCoin\":100}"));
        assertEquals("money_overflow", response.getString("status"));
        assertFalse(response.getBooleanValue("retryable"));
        assertFalse(response.getBooleanValue("manualReview"));
        assertFalse(player.getBool("RewardedAdTokenClaimed"));
        assertFalse(player.getBool("RewardedAdClaimReview"));
        assertEquals(Long.MAX_VALUE, properties.get(PLAYER_MONEY));
    }

    @Test
    public void p3VerifyServerErrorExplicitlyDisablesAutomaticRetry() {
        failPropertyOnce = "RewardedAdDay";
        new AdModule().OnReqRewardedAdVerify(kernel, player, 10021, 1, bytes("{\"adResult\":\"Finished\"}"));
        assertEquals(8, response.getIntValue("code"));
        assertEquals("server_error", response.getString("status"));
        assertFalse(response.getBooleanValue("valid"));
        assertEquals(Boolean.FALSE, response.get("retryable"));
        assertFalse(properties.containsKey("RewardedAdToken"));
    }

    private JSONObject finishRequest(String id, String resolution, boolean win) {
        JSONObject request = new JSONObject();
        request.put("attemptId", id); request.put("levelId", 1); request.put("coin", 50);
        request.put("resolution", resolution); request.put("isWin", win);
        return request;
    }

    private void submit(JSONObject request) throws Exception {
        levels.OnReqLevelFinish(kernel, player, 10005, 1, bytes(request.toJSONString()));
    }

    private String start(int level) throws Exception {
        levels.OnReqLevelStart(kernel, player, 10006, 1,
                bytes("{\"levelId\":" + level + ",\"settlementVersion\":2}"));
        return response == null ? null : response.getString("attemptId");
    }

    private void finish(String id, int level, boolean win, String resolution) throws Exception {
        JSONObject request = new JSONObject();
        if (id != null) request.put("attemptId", id);
        request.put("levelId", level);
        request.put("coin", 500); // Server cap remains authoritative.
        request.put("isWin", win);
        request.put("resolution", resolution);
        levels.OnReqLevelFinish(kernel, player, 10005, 1, bytes(request.toJSONString()));
    }

    private static byte[] bytes(String json) {
        return CustomMsg.String.newBuilder().setValue(json).build().toByteArray();
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        return null;
    }
}
