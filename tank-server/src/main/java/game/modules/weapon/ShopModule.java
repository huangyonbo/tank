package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import framework.SystemConfigData;
import framework.game.ICfgReader;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.game.ILogicModule;
import framework.game.IRecord;
import framework.game.KernelEvent;
import framework.game.ValueType;
import framework.mybatis.domain.GooglePlayOrder;
import framework.mybatis.service.impl.GooglePlayOrderService;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Google Play 濠电偞鍨甸悧鎰板焵椤掍緡娈旈柣搴ｅ厴閺屽矂骞嬪┑鍫紙闂佸摜鍠庡Λ妤呭箹瑜旈幆鍐礋椤撶儐浠剧紓浣规煥椤戝懏顨滄潏鈺冩／闁挎柧鑼庢笟鈧畷绋款渻鐏忔牕浜? *
 * <p>闂佸搫鐗滈崜姘冲暞闂佺鍕╀沪缂佹顦遍幏鐘垫嫚閹绘帞鎮奸梺?StoreGold/ItemPkg 闂備焦婢樼粔鍫曟偪閸℃稒鏅悘鐐跺亹閻﹀秴鈽夐幘宕囆ラ柣銊у枛閹粙濡搁敂鏂ょ礈婵☆偅婢樼€氼亞绮崨鏉戠闁哄洦菤閸嬫挻鎷呯憴鍕婵炲濮甸敋婵炲弶鐗滈幏顐﹀礃閳轰胶浠?MallsModule 闂佸憡鐟﹂崹濂稿箵閿濆鐒婚柡鍕箳鐢棝鏌?/p>
 */
public class ShopModule implements ILogicModule {
    private static final Logger logger = LoggerFactory.getLogger(ShopModule.class);

    private static final String GOOGLE_PLAY_PRODUCTS_PATH = "res/GooglePlay/GooglePlayProducts.xml";
    private static final String GOOGLE_PLAY_ORDER_REC = "GooglePlayOrderRec";
    private static final String PLAYER_HEAD_REC = "RecHadHead";
    private static final int GOOGLE_PLAY_ORDER_REC_MAX_ROWS = 200;

    private enum OrderRecCol {
        TOKEN_HASH(0, ValueType.STRING),
        STATUS(1, ValueType.INT),
        PRODUCT_ID(2, ValueType.STRING),
        GOOGLE_ORDER_ID(3, ValueType.STRING),
        UPDATE_TIME(4, ValueType.LONG),
        CONSUMED(5, ValueType.INT),
        REWARD_STEP(6, ValueType.INT),
        REWARD_VERSION(7, ValueType.INT),
        PRICE_YUAN(8, ValueType.INT),
        GOLD_AMOUNT(9, ValueType.LONG),
        HEAD_ID(10, ValueType.INT),
        HEAD_HOURS(11, ValueType.INT),
        FULFILLMENT_HASH(12, ValueType.STRING),
        RECHARGE_APPLIED(13, ValueType.INT),
        MIN_MONTH_AFTER_RECHARGE(14, ValueType.INT),
        MANUAL_REVIEW(15, ValueType.INT);

        private final int index;
        private final ValueType type;

        OrderRecCol(int index, ValueType type) {
            this.index = index;
            this.type = type;
        }
    }

    private enum HeadRecCol {
        ID(0),
        END_TIME(1);

        private final int index;

        HeadRecCol(int index) {
            this.index = index;
        }
    }

    private static final int PAY_STATUS_VERIFIED = 1;
    private static final int PAY_STATUS_REWARDS_GRANTED = 2;
    /** 婵犻潧鍊归悧鏇熸叏閹惰棄妞介悘鐐舵鐢娀鏌涙繝鍛汗缂侇喛娅ｉ幏瀣灳閾忣偆绉甸悗鐟版啞瑜板啴宕幘璇茬闁靛／鍐闁诲海顢婂▔娑㈠箵閳哄懏鍋傜憸婵堟濠靛牏鐭夊ù锝堫嚃閸撻箖鏌涢幇顒傂ｆい鎰偢瀹曪綁骞嬮敐鍡楃稉闂?*/
    private static final int PAY_STATUS_AWARDED = 3;
    /** 闂佺绻堥崝宀勬儑椤掑倹濯奸柕蹇曞Т缁€瀣偓瑙勭摃鐏忔瑩鍨惧Ο鍏煎?SHIPPED闂佹寧绋戦懟顖濄亹閼碱兘鍋撻悷閭︽Ц闁告瑥绻愯彁婵☆垵顕у畷娆撳级閸繂鏆㈤柍褜鍏涢懗璺衡枔閹达箑瀚夋い鎺嗗亾婵犫偓閹绢喖鍌ㄩ柛鈩冾殔閽戝鏌?*/
    private static final int PAY_STATUS_SHIPPED = 4;
    private static final int PAY_CONSUMED = 1;

    private static final long LOCAL_LOCK_LEASE_MS = 5L * 60 * 1000;
    private static final long LEASE_ACK_FRESH_MS = 30L * 1000;
    private static final long LEASE_HEARTBEAT_SECONDS = 60L;
    private static final long LEASE_HEARTBEAT_STALE_MS = 4L * 60 * 1000;
    private static final int MAX_VERIFY_IN_FLIGHT_PER_UID = 2;
    private static final String ACCOUNT_ID_PREFIX = "google-play:";

    private volatile Map<String, GooglePlayProduct> googlePlayProducts = Collections.emptyMap();
    private final GooglePlayPurchaseVerifier googleVerifier = new GooglePlayPurchaseVerifier();
    private final ConcurrentMap<String, LocalProcessingLock> localProcessing = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LeaseHeartbeat> leaseHeartbeats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, AtomicInteger> verifyingByUid = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, String> fulfillmentByUid = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> fulfillmentUidByOwner = new ConcurrentHashMap<>();

    private final ExecutorService googlePlayIo = new ThreadPoolExecutor(
            2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(128), runnable -> {
        Thread thread = new Thread(runnable, "google-play-io");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private final ScheduledExecutorService leaseHeartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "google-play-lease-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
        kernel.regRequestMessage(RequestMsgDef.REQ_GOOGLE_PLAY_PRODUCTS.getId(), this, "OnGooglePlayProducts");
        kernel.regRequestMessage(RequestMsgDef.REQ_GOOGLE_PLAY_PREPARE.getId(), this, "OnGooglePlayPrepare");
        kernel.regRequestMessage(RequestMsgDef.REQ_GOOGLE_PLAY_BUY_GOLD.getId(), this, "OnGooglePlayBuyGold");
        loadGooglePlayProducts(kernel);

        if (isBlank(getConfiguredPackageName())) {
            logger.error("googlePlayPackageName is empty; Google Play purchases will be rejected");
        }
        if (!hasValidAccountIdSecret()) {
            logger.error("googlePlayAccountIdSecret must be a server-only secret of at least 32 characters");
        }
        return true;
    }

    @Override
    public void onDestroy() {
        for (LeaseHeartbeat heartbeat : leaseHeartbeats.values()) {
            heartbeat.future.cancel(false);
        }
        leaseHeartbeats.clear();
        localProcessing.clear();
        verifyingByUid.clear();
        fulfillmentByUid.clear();
        fulfillmentUidByOwner.clear();
        leaseHeartbeatExecutor.shutdownNow();
        googlePlayIo.shutdownNow();
    }

    public void OnPlayerClassCreate(IKernel kernel, String script) {
        IRecord rec = kernel.declareRecord(script, GOOGLE_PLAY_ORDER_REC, OrderRecCol.values().length,
                GOOGLE_PLAY_ORDER_REC_MAX_ROWS, false, false, true);
        for (OrderRecCol col : OrderRecCol.values()) {
            rec.setColType(col.index, col.type);
        }
    }

    public void RefreshCfg(IKernel kernel, String path) {
        if (path == null || path.endsWith("GooglePlayProducts.xml")) {
            loadGooglePlayProducts(kernel);
        }
    }

    /** 闁哄鏅滈弻銊ッ?Google Play 闂佸搫鍊烽崡鎶藉箺閼测晜宕夋い鏍ㄧ懅閻熲晠鎮橀悙闈涗粶闁哄懌鍨藉畷顐ｇ瑹婵犲嫮顦繛鎴炴尭缁夐潧袙濮樿埖顥嗛柛娆忣槸閻忔鎮归崶顏嶅殭鐟滄澘娲顔款槻闁哄懌鍨藉畷鐑樻償閳╁啯鐝滈梺鍛婄箑缁€渚€宕归鍡樺仒闁靛绲洪崑?*/
    public void OnGooglePlayProducts(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        JSONArray products = new JSONArray();
        for (GooglePlayProduct product : googlePlayProducts.values()) {
            if (!product.enabled) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("productId", product.productId);
            item.put("rewardVersion", product.rewardVersion);
            item.put("balanceProperty", PLAYER_MONEY);
            item.put("gold", product.goldAmount);
            item.put("headId", product.headId);
            item.put("headHours", product.headHours);
            products.add(item);
        }
        resp.put("products", products);
        respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                "ok", "", "");
    }

    /** 闁荤姵鍔﹂崢娲箯闁秴绀堢€广儱鐗忛妶顐︽煛瀹ュ懎鎮戞い銉ユ嚇瀵濡烽婊咁槷濡ょ姷鍋炲﹢鍦箔閸涙潙鐭楅柟瀛樼箓濞懷囨煟椤剙濡虹紒?BillingFlowParams 闂佹眹鍔岀€氼厼锕㈤崶顒€绀夐柨娑樺娴?HMAC 闁荤姵鍔х粻鎴ｃ亹閸ф鍐€闁搞儺浜炲Σ鏇㈡煏?*/
    public void OnGooglePlayPrepare(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        try {
            CustomMsg.String msg = CustomMsg.String.parseFrom(data);
            JSONObject req = JSON.parseObject(msg.getValue());
            String productId = req == null ? "" : trim(req.getString("productId"));
            if (!validProductId(productId)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR,
                        "invalid_request", "", "");
                return;
            }

            GooglePlayProduct product = googlePlayProducts.get(productId);
            if (product == null || !product.enabled) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_NOT_EXIST,
                        "product_not_found", "", "");
                return;
            }
            if (isBlank(getConfiguredPackageName()) || !hasValidAccountIdSecret()) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "server_not_configured", "", "");
                return;
            }

            int uid = player.getInt(PLAYER_PROPERTY_UID);
            if (uid <= 0) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "invalid_player", "", "");
                return;
            }
            String restriction = checkPurchaseEligibility(player, product.priceYuan);
            if (restriction != null) {
                logger.info("Google Play prepare blocked uid={}, productId={}, restriction={}, "
                                + "enforceMinorLimits={}, testPayConfig={}, playerTestPay={}, "
                                + "certification={}, age={}, monthRecharge={}",
                        uid, productId, restriction,
                        getBooleanConfig("googlePlayEnforceMinorLimits", true),
                        getBooleanConfig("testPay", false),
                        player.getBool(PLAYER_PROPERTY_TESTPAY),
                        player.getBool(PLAYER_PROPERTY_CERTIFICATION),
                        player.getInt(PLAYER_PROPERTY_AGE),
                        player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT));
                resp.put("eligible", false);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                        restriction, "", "");
                return;
            }

            resp.put("productId", productId);
            resp.put("eligible", true);
            resp.put("obfuscatedAccountId", expectedAccountId(uid));
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                    "ready", "", "");
        } catch (Exception e) {
            logger.error("OnGooglePlayPrepare error", e);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", "", "");
        }
    }

    public void OnGooglePlayBuyGold(IKernel kernel, IGameObject player,
                                    int msgid, int reqid, byte[] data) {
        JSONObject resp = new JSONObject();
        String productId = "";
        String purchaseToken = "";
        String tokenHash = "";
        String processingOwner = "";

        try {
            CustomMsg.String msg = CustomMsg.String.parseFrom(data);
            JSONObject req = JSON.parseObject(msg.getValue());
            if (req == null) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR,
                        "invalid_request", tokenHash, processingOwner);
                return;
            }

            productId = trim(req.getString("productId"));
            purchaseToken = trim(req.getString("purchaseToken"));
            String packageName = getConfiguredPackageName();
            if (isBlank(packageName) || !hasValidAccountIdSecret()) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "server_not_configured", tokenHash, processingOwner);
                return;
            }
            if (!validProductId(productId)
                    || isBlank(purchaseToken) || purchaseToken.length() > 4096) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR,
                        "invalid_request", tokenHash, processingOwner);
                return;
            }

            int uid = player.getInt(PLAYER_PROPERTY_UID);
            if (uid <= 0) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "invalid_player", tokenHash, processingOwner);
                return;
            }

            tokenHash = sha256Hex(purchaseToken);
            resp.put("productId", productId);
            resp.put("verificationId", shortHash(tokenHash));
            processingOwner = UUID.randomUUID().toString();
            if (!acquireLocalLock(tokenHash, processingOwner)) {
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                        "processing", "", "");
                return;
            }

            IRecord rec = player.getRecord(GOOGLE_PLAY_ORDER_REC);
            if (rec == null) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "payment_record_missing", tokenHash, processingOwner);
                return;
            }

            int row = findLocalRow(rec, tokenHash);
            GooglePlayProduct product;
            if (row != -1) {
                if (!productId.equals(rec.getString(row, OrderRecCol.PRODUCT_ID.index))) {
                    respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                            "token_product_mismatch", tokenHash, processingOwner);
                    return;
                }
                product = productFromLocalRecord(rec, row);
                if (product == null) {
                    respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                            "payment_record_corrupt", tokenHash, processingOwner);
                    return;
                }
            } else {
                // A product already paid through Billing must remain fulfillable even if later disabled.
                product = googlePlayProducts.get(productId);
                if (product == null) {
                    respond(kernel, player, reqid, resp, ServerCodeDef.CODE_NOT_EXIST,
                            "product_not_found", tokenHash, processingOwner);
                    return;
                }
            }

            if (row != -1 && rec.getInt(row, OrderRecCol.STATUS.index) >= PAY_STATUS_AWARDED) {
                recoverCompletedOrder(kernel, player, reqid, resp, packageName, product,
                        purchaseToken, tokenHash, processingOwner, rec);
                return;
            }
            if (row != -1 && rec.getInt(row, OrderRecCol.MANUAL_REVIEW.index) == 1) {
                persistManualReview(kernel, player, reqid, resp, tokenHash,
                        processingOwner, uid, rec, row);
                return;
            }

            verifyPurchaseAsync(kernel, player, reqid, resp, packageName, product,
                    purchaseToken, tokenHash, processingOwner, uid, rec);
        } catch (Exception e) {
            logger.error("OnGooglePlayBuyGold error productId={} token={}",
                    productId, mask(purchaseToken), e);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", tokenHash, processingOwner);
        }
    }

    private void verifyPurchaseAsync(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                                     String packageName, GooglePlayProduct product, String purchaseToken,
                                     String tokenHash, String processingOwner, int uid, IRecord rec) {
        if (!tryAcquireVerification(uid)) {
            resp.put("retryable", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                    "verification_busy", tokenHash, processingOwner);
            return;
        }
        try {
            googlePlayIo.execute(() -> {
                try {
                    GooglePlayPurchaseVerifier.Purchase purchase = googleVerifier.getProductPurchase(
                            packageName, product.productId, purchaseToken);
                    kernel.runOnLogicThread(() -> handleVerifiedPurchase(kernel, player, reqid, resp,
                            packageName, product, purchaseToken, tokenHash, processingOwner,
                            uid, rec, purchase));
                } catch (Exception e) {
                    kernel.runOnLogicThread(() -> {
                        logger.error("verify Google Play purchase failed productId={} token={}",
                                product.productId, mask(purchaseToken), e);
                        if (isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                            resp.put("retryable", true);
                            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                    "google_api_error", tokenHash, processingOwner);
                        } else {
                            abandonLocalLock(tokenHash, processingOwner);
                        }
                    });
                } finally {
                    releaseVerification(uid);
                }
            });
        } catch (RejectedExecutionException e) {
            releaseVerification(uid);
            logger.warn("Google Play IO queue is full uid={} productId={}", uid, product.productId);
            resp.put("retryable", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "service_busy", tokenHash, processingOwner);
        }
    }

    private void handleVerifiedPurchase(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                                        String packageName, GooglePlayProduct product, String purchaseToken,
                                        String tokenHash, String processingOwner, int uid, IRecord rec,
                                        GooglePlayPurchaseVerifier.Purchase purchase) {
        try {
            if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                abandonLocalLock(tokenHash, processingOwner);
                return;
            }
            if (purchase.purchaseState != 0) {
                resp.put("purchaseState", purchase.purchaseState);
                String status = purchase.purchaseState == 2 ? "pending" : "not_purchased";
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        status, tokenHash, processingOwner);
                return;
            }
            if (!isBlank(purchase.productId) && !product.productId.equals(purchase.productId)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR,
                        "google_product_mismatch", tokenHash, processingOwner);
                return;
            }
            if (!isBlank(purchase.purchaseToken) && !purchaseToken.equals(purchase.purchaseToken)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PARAM_ERR,
                        "google_token_mismatch", tokenHash, processingOwner);
                return;
            }
            if (purchase.quantity != 1) {
                resp.put("quantity", purchase.quantity);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "unsupported_quantity", tokenHash, processingOwner);
                return;
            }

            String expectedAccountId = expectedAccountId(uid);
            if ((requireObfuscatedAccountId() && isBlank(purchase.obfuscatedExternalAccountId))
                    || (!isBlank(purchase.obfuscatedExternalAccountId)
                    && !expectedAccountId.equals(purchase.obfuscatedExternalAccountId))) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "account_mismatch", tokenHash, processingOwner);
                return;
            }

            int row = saveVerifiedLocalOrder(rec, tokenHash, product, purchase.orderId,
                    kernel.getServerTime());
            if (row == -1) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "payment_record_full", tokenHash, processingOwner);
                return;
            }
            if (!localSnapshotMatches(rec, row, product)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "fulfillment_config_mismatch", tokenHash, processingOwner);
                return;
            }

            GooglePlayOrder order = buildGlobalOrder(tokenHash, uid, packageName, product,
                    purchase.orderId, expectedAccountId, purchase.purchaseTimeMillis,
                    purchase.quantity, GooglePlayOrder.STATUS_VERIFIED, processingOwner);
            boolean googleAlreadyConsumed = purchase.consumptionState == PAY_CONSUMED;
            kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "claimAndAcquire",
                    Collections.singletonList(order), result -> handleClaimResult(kernel, player,
                            reqid, resp, packageName, product, purchaseToken, tokenHash,
                            processingOwner, uid, googleAlreadyConsumed, result));
        } catch (Exception e) {
            logger.error("handleVerifiedPurchase error productId={} token={}",
                    product.productId, mask(purchaseToken), e);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", tokenHash, processingOwner);
        }
    }

    private void handleClaimResult(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                                   String packageName, GooglePlayProduct requestedProduct,
                                   String purchaseToken, String tokenHash, String processingOwner,
                                   int uid, boolean googleAlreadyConsumed, String result) {
        try {
            if (isBlank(result)) {
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "delivery_db_unavailable", tokenHash, processingOwner);
                return;
            }

            GooglePlayOrder saved = JSON.parseObject(result, GooglePlayOrder.class);
            if (!sameGlobalIdentity(saved, tokenHash, uid, packageName,
                    requestedProduct.productId)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "token_already_claimed", tokenHash, processingOwner);
                return;
            }
            GooglePlayProduct product = productFromGlobalOrder(saved);
            if (product == null) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "fulfillment_config_mismatch", tokenHash, processingOwner);
                return;
            }

            if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                if (isOwnedProcessingOrder(saved, processingOwner)) {
                    releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                }
                abandonLocalLock(tokenHash, processingOwner);
                return;
            }

            IRecord rec = player.getRecord(GOOGLE_PLAY_ORDER_REC);
            int localRow = findLocalRow(rec, tokenHash);
            if (localRow == -1) {
                localRow = saveVerifiedLocalOrder(rec, tokenHash, product,
                        saved.getGoogleOrderId(), kernel.getServerTime());
            } else if (!reconcileUntouchedLocalSnapshot(rec, localRow, product)) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "fulfillment_config_mismatch", tokenHash, processingOwner);
                return;
            }
            if (localRow == -1) {
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "payment_record_full", tokenHash, processingOwner);
                return;
            }

            if (saved.getStatus() != null
                    && saved.getStatus() == GooglePlayOrder.STATUS_MANUAL_REVIEW) {
                rec.setValue(localRow, OrderRecCol.MANUAL_REVIEW.index, 1);
                persistManualReview(kernel, player, reqid, resp, tokenHash,
                        processingOwner, uid, rec, localRow);
                return;
            }
            if (isCompletedGlobalStatus(saved.getStatus())) {
                int consumed = saved.getStatus() == GooglePlayOrder.STATUS_CONSUMED
                        ? PAY_CONSUMED : 0;
                int row = saveCompletedLocalOrder(rec, tokenHash, product,
                        saved.getGoogleOrderId(), kernel.getServerTime(),
                        PAY_STATUS_SHIPPED, consumed);
                if (row == -1) {
                    respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                            "payment_record_full", tokenHash, processingOwner);
                    return;
                }
                flushRecoveredOrder(kernel, player, reqid, resp, packageName, product,
                        purchaseToken, tokenHash, processingOwner, rec, true);
                return;
            }

            if (!isOwnedProcessingOrder(saved, processingOwner)) {
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                        "processing", tokenHash, processingOwner);
                return;
            }
            if (googleAlreadyConsumed) {
                releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                        "consumed_without_fulfillment", tokenHash, processingOwner);
                return;
            }

            long renewRequestNanos = System.nanoTime();
            kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "renewLease",
                    Arrays.asList(tokenHash, uid, processingOwner), renewed -> {
                        long renewedAt = parseLong(renewed);
                        long ackDelay = TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime() - renewRequestNanos);
                        if (renewedAt <= 0 || ackDelay > LEASE_ACK_FRESH_MS) {
                            releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                            resp.put("retryable", true);
                            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                                    renewedAt > 0 ? "lease_ack_stale" : "processing",
                                    tokenHash, processingOwner);
                            return;
                        }
                        if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                            releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                            abandonLocalLock(tokenHash, processingOwner);
                            return;
                        }
                        dispatchClaimedOrder(kernel, player, reqid, resp, packageName,
                                product, purchaseToken, tokenHash, processingOwner, uid,
                                saved.getGoogleOrderId());
                    });
        } catch (Exception e) {
            logger.error("handleClaimResult error productId={} token={}",
                    requestedProduct.productId, mask(purchaseToken), e);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", tokenHash, processingOwner);
        }
    }

    private void dispatchClaimedOrder(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                                      String packageName, GooglePlayProduct product,
                                      String purchaseToken, String tokenHash,
                                      String processingOwner, int uid, String googleOrderId) {
        try {
            if (!acquireFulfillment(uid, processingOwner)) {
                releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                        "player_fulfillment_busy", tokenHash, processingOwner);
                return;
            }

            IRecord rec = player.getRecord(GOOGLE_PLAY_ORDER_REC);
            int row = findLocalRow(rec, tokenHash);
            if (row == -1 || !localSnapshotMatches(rec, row, product)) {
                releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "payment_record_missing", tokenHash, processingOwner);
                return;
            }
            if (rec.getInt(row, OrderRecCol.STATUS.index) >= PAY_STATUS_AWARDED) {
                recoverCompletedOrder(kernel, player, reqid, resp, packageName, product,
                        purchaseToken, tokenHash, processingOwner, rec);
                return;
            }
            if (rec.getInt(row, OrderRecCol.MANUAL_REVIEW.index) == 1) {
                persistManualReview(kernel, player, reqid, resp, tokenHash,
                        processingOwner, uid, rec, row);
                return;
            }

            if (!startLeaseHeartbeat(kernel, tokenHash, uid, processingOwner)) {
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "lease_heartbeat_unavailable", tokenHash, processingOwner);
                return;
            }
            continueRewardFulfillment(kernel, player, reqid, resp, packageName, product,
                    purchaseToken, tokenHash, processingOwner, uid, googleOrderId, rec);
        } catch (Exception e) {
            logger.error("dispatchClaimedOrder error productId={} token={}",
                    product.productId, mask(purchaseToken), e);
            resp.put("retryable", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "server_error", tokenHash, processingOwner);
        }
    }

    /**
     * 濠殿噯绲界换鎰版偩椤掑嫬绠ｉ柟閭︿簽椤忓崬鈽夐幙鍐х敖閻庢凹鍘剧划鈺冣偓锝庝憾濞堫垶鏌涢弬鍨埞婵ǜ鍔岄锝夊传閸曨偆鍘繛锝呮处缁诲倿鎮?rewardStep闂侀潧妫楅崐鍧楀闯閸濄儲瀚氶柡鍥╁仜濞懷呯磽娴ｅ牆鎳愰弫鎯р槈閹炬剚鍎庣紒鏃€娼欓～銏ゅΨ椤栨粎顦繛鎴炴尭缁夊磭妲愭导瀛樼厒鐎广儱鎷嬪Σ璇差熆瑜忛崑娑欐叏閻愮儤鐓傞柟瀛樼箘椤忔煡鏌熺€涙澧柕鍥ㄦ瀹曟捁绠涢弮鈧ˇ褔姊婚埀顒傛啑閵堝倸浜?     */
    private void continueRewardFulfillment(IKernel kernel, IGameObject player, int reqid,
                                           JSONObject resp, String packageName,
                                           GooglePlayProduct product, String purchaseToken,
                                           String tokenHash, String processingOwner, int uid,
                                           String googleOrderId, IRecord rec) {
        try {
            continueRewardFulfillmentInternal(kernel, player, reqid, resp, packageName,
                    product, purchaseToken, tokenHash, processingOwner, uid,
                    googleOrderId, rec);
        } catch (Exception e) {
            logger.error("continue Google Play fulfillment failed uid={} productId={} token={}",
                    uid, product.productId, shortHash(tokenHash), e);
            persistManualReview(kernel, player, reqid, resp, tokenHash,
                    processingOwner, uid, rec, findLocalRow(rec, tokenHash));
        }
    }

    private void continueRewardFulfillmentInternal(IKernel kernel, IGameObject player, int reqid,
                                                   JSONObject resp, String packageName,
                                                   GooglePlayProduct product, String purchaseToken,
                                                   String tokenHash, String processingOwner, int uid,
                                                   String googleOrderId, IRecord rec) {
        if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
            abandonLocalLock(tokenHash, processingOwner);
            return;
        }
        if (!hasValidLeaseHeartbeat(tokenHash, processingOwner)) {
            resp.put("retryable", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_PROTECTED,
                    "lease_lost", tokenHash, processingOwner);
            return;
        }

        int row = findLocalRow(rec, tokenHash);
        if (row == -1 || !localSnapshotMatches(rec, row, product)) {
            resp.put("retryable", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "payment_record_corrupt", tokenHash, processingOwner);
            return;
        }
        if (rec.getInt(row, OrderRecCol.MANUAL_REVIEW.index) == 1) {
            resp.put("manualReview", true);
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_WRONG_STATE,
                    "reward_state_unknown", tokenHash, processingOwner);
            return;
        }

        int rewardStep = rec.getInt(row, OrderRecCol.REWARD_STEP.index);
        int rewardSteps = product.rewardSteps();
        if (rewardStep < 0 || rewardStep > rewardSteps) {
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "payment_record_corrupt", tokenHash, processingOwner);
            return;
        }

        if (rewardStep < rewardSteps) {
            GrantResult grantResult = rewardStep == 0
                    ? grantMoney(player, product.goldAmount)
                    : grantHead(kernel, player, product.headId, product.headHours);
            if (grantResult == GrantResult.UNKNOWN) {
                rec.setValue(row, OrderRecCol.MANUAL_REVIEW.index, 1);
                rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
                persistManualReview(kernel, player, reqid, resp, tokenHash,
                        processingOwner, uid, rec, row);
                return;
            }
            if (grantResult == GrantResult.NO_EFFECT) {
                releaseGlobalForRetry(kernel, tokenHash, uid, processingOwner);
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "reward_dispatch_failed", tokenHash, processingOwner);
                return;
            }

            rec.setValue(row, OrderRecCol.REWARD_STEP.index, rewardStep + 1);
            rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
            kernel.flushGooglePlayPlayer(player, flushed -> {
                if (!Boolean.TRUE.equals(flushed)) {
                    if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                        abandonLocalLock(tokenHash, processingOwner);
                        return;
                    }
                    resp.put("retryable", true);
                    respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                            "player_save_pending", tokenHash, processingOwner);
                    return;
                }
                continueRewardFulfillment(kernel, player, reqid, resp, packageName,
                        product, purchaseToken, tokenHash, processingOwner, uid,
                        googleOrderId, rec);
            });
            return;
        }

        rec.setValue(row, OrderRecCol.STATUS.index, Math.max(rec.getInt(row, OrderRecCol.STATUS.index),
                PAY_STATUS_REWARDS_GRANTED));
        GrantResult rechargeResult = applyRechargeProperties(player, product.priceYuan, rec, row);
        if (rechargeResult == GrantResult.UNKNOWN) {
            persistManualReview(kernel, player, reqid, resp, tokenHash,
                    processingOwner, uid, rec, row);
            return;
        }
        if (rechargeResult == GrantResult.NO_EFFECT) {
            // Persist the partial recharge state with the player snapshot.
            rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
            kernel.flushGooglePlayPlayer(player, flushed -> {
                if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                    abandonLocalLock(tokenHash, processingOwner);
                    return;
                }
                resp.put("retryable", true);
                resp.put("partialProgressSaved", Boolean.TRUE.equals(flushed));
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "recharge_tracking_failed", tokenHash, processingOwner);
            });
            return;
        }

        rec.setValue(row, OrderRecCol.STATUS.index, PAY_STATUS_AWARDED);
        rec.setValue(row, OrderRecCol.GOOGLE_ORDER_ID.index, nullToEmpty(googleOrderId));
        rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
        kernel.flushGooglePlayPlayer(player, flushed -> {
            if (!Boolean.TRUE.equals(flushed)) {
                if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                    abandonLocalLock(tokenHash, processingOwner);
                    return;
                }
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "player_save_pending", tokenHash, processingOwner);
                return;
            }
            markGlobalShippedAndFinish(kernel, player, reqid, resp, packageName,
                    product, purchaseToken, tokenHash, processingOwner, uid, false);
        });
    }

    private void persistManualReview(IKernel kernel, IGameObject player, int reqid,
                                     JSONObject resp, String tokenHash,
                                     String processingOwner, int uid,
                                     IRecord rec, int row) {
        try {
            if (rec != null && row >= 0) {
                rec.setValue(row, OrderRecCol.MANUAL_REVIEW.index, 1);
                rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
            }
        } catch (Exception e) {
            logger.error("mark local Google Play order for manual review failed uid={} token={}",
                    uid, shortHash(tokenHash), e);
        }
        kernel.executeGooglePlayToStore(GooglePlayOrderService.class,
                "markManualReview", Arrays.asList(tokenHash, uid), marked -> {
                    final boolean globalMarked = parseBoolean(marked);
                    if (!isCurrentRequest(kernel, player, uid,
                            tokenHash, processingOwner)) {
                        abandonLocalLock(tokenHash, processingOwner);
                        return;
                    }
                    kernel.flushGooglePlayPlayer(player, flushed -> {
                        if (!isCurrentRequest(kernel, player, uid,
                                tokenHash, processingOwner)) {
                            abandonLocalLock(tokenHash, processingOwner);
                            return;
                        }
                        resp.put("manualReview", true);
                        if (!globalMarked) {
                            resp.put("retryable", true);
                            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                    "manual_review_record_pending", tokenHash,
                                    processingOwner);
                            return;
                        }
                        if (!Boolean.TRUE.equals(flushed)) {
                            resp.put("retryable", true);
                            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                    "manual_review_save_pending", tokenHash,
                                    processingOwner);
                            return;
                        }
                        respond(kernel, player, reqid, resp,
                                ServerCodeDef.CODE_WRONG_STATE,
                                "reward_state_unknown", tokenHash, processingOwner);
                    });
                });
    }

    private GrantResult grantMoney(IGameObject player, long amount) {
        long before = player.getLong(PLAYER_MONEY);
        long expected;
        try {
            expected = Math.addExact(before, amount);
        } catch (ArithmeticException e) {
            logger.error("Google Play money overflow uid={} before={} amount={}",
                    player.getInt(PLAYER_PROPERTY_UID), before, amount);
            return GrantResult.NO_EFFECT;
        }

        try {
            player.setProperty(PLAYER_MONEY, expected,
                    IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                    "google iap reward amount=" + amount);
        } catch (Exception e) {
            logger.error("Google Play money listener failed uid={} expected={}",
                    player.getInt(PLAYER_PROPERTY_UID), expected, e);
        }
        long actual = player.getLong(PLAYER_MONEY);
        if (actual == expected) {
            return GrantResult.GRANTED;
        }
        return actual == before ? GrantResult.NO_EFFECT : GrantResult.UNKNOWN;
    }

    private GrantResult grantHead(IKernel kernel, IGameObject player, int headId, int headHours) {
        if (headId <= 0) {
            return GrantResult.GRANTED;
        }
        IRecord headRec = player.getRecord(PLAYER_HEAD_REC);
        if (headRec == null || headHours <= 0) {
            logger.error("Google Play head record/config missing uid={} headId={} hours={}",
                    player.getInt(PLAYER_PROPERTY_UID), headId, headHours);
            return GrantResult.NO_EFFECT;
        }

        int row = headRec.findRow(0, HeadRecCol.ID.index, headId);
        boolean hadRow = row != -1;
        long previousEnd = hadRow ? headRec.getLong(row, HeadRecCol.END_TIME.index) : 0L;
        long expectedEnd;
        if (hadRow && previousEnd == -1L) {
            expectedEnd = -1L;
        } else {
            long base = Math.max(kernel.getServerTime(), previousEnd);
            try {
                expectedEnd = Math.addExact(base, Math.multiplyExact((long) headHours, 3600000L));
            } catch (ArithmeticException e) {
                logger.error("Google Play head expiry overflow uid={} headId={} hours={}",
                        player.getInt(PLAYER_PROPERTY_UID), headId, headHours);
                return GrantResult.NO_EFFECT;
            }
        }

        try {
            if (row == -1) {
                if (!headRec.addRow(headId, expectedEnd)) {
                    return GrantResult.NO_EFFECT;
                }
                row = headRec.findRow(0, HeadRecCol.ID.index, headId);
            } else if (expectedEnd != -1L) {
                headRec.setValue(row, HeadRecCol.END_TIME.index, expectedEnd);
            }
            player.setProperty(PLAYER_PROPERTY_HEADID, headId);
        } catch (Exception e) {
            logger.error("Google Play head grant listener failed uid={} headId={}",
                    player.getInt(PLAYER_PROPERTY_UID), headId, e);
        }

        int actualRow = headRec.findRow(0, HeadRecCol.ID.index, headId);
        if (actualRow != -1
                && headRec.getLong(actualRow, HeadRecCol.END_TIME.index) == expectedEnd) {
            return GrantResult.GRANTED;
        }
        if ((!hadRow && actualRow == -1)
                || (hadRow && actualRow != -1
                && headRec.getLong(actualRow, HeadRecCol.END_TIME.index) == previousEnd)) {
            return GrantResult.NO_EFFECT;
        }
        return GrantResult.UNKNOWN;
    }

    /**
     * 闂佸搫娲ら悺銊╁蓟婵犲洦鐒绘慨妯块哺閺嗗繘鏌ｅ┃鐐珔妞ゆ柨绉瑰畷妤呭川椤撴稑浜鹃柤娴嬫櫇閸╃娀鎮规担鍙夘潐缂佽鲸绻冮幏鍛村幢閺囩姷鎲归柣鐔哥懕缁犳垼銇愰崒鐐茬睄鐟滃酣寮鐣岊浄婵☆垳鍎ら崵鎺懨归悩顔尖偓鏍垂閵娾晛绫嶇憸搴ㄥ极椤旂晫顩锋俊顖滃劋缁€鈧柣鐘差儏閸熴劑鍩€?     * 闁诲繒鍋熼崑鐐哄焵椤戭剙鎳忕壕褔鏌涘鐓庡缂佽鲸鎸鹃弫顔剧矙閹稿骸鈧潧霉閻橆喖鍔氶柣銈呭濮婁粙宕ㄩ鐔封偓鐢电磽閸愭儳鏋涢柍褜鍓涢崢褔宕规禒瀣妞ゆ挶鍔庣粈澶愭⒑椤掆偓閻忔繈宕㈤妶澶婄鐎广儱妫崑褏绱掓鏍х仩濠殿喚鍋熼埀顑跨祷婢瑰牓宕佃瀹曪絽鐣濋崘鎯ф倎闂?     */
    private GrantResult applyRechargeProperties(IGameObject player, int priceYuan,
                                                IRecord rec, int row) {
        int minMonthAfterRecharge;
        if (rec.getInt(row, OrderRecCol.RECHARGE_APPLIED.index) != 1) {
            int beforeTotal = player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT);
            int beforeMonth = player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT);
            int expectedTotal = saturatedAdd(beforeTotal, priceYuan);
            minMonthAfterRecharge = saturatedAdd(beforeMonth, priceYuan);

            try {
                player.setProperty(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT, expectedTotal);
            } catch (Exception e) {
                logger.error("Google Play recharge property listener failed uid={} expectedTotal={}",
                        player.getInt(PLAYER_PROPERTY_UID), expectedTotal, e);
            }
            if (player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT) != expectedTotal) {
                return player.getInt(PLAYER_PROPERTY_TOTALRECHARGEAMOUNT) == beforeTotal
                        ? GrantResult.NO_EFFECT : GrantResult.UNKNOWN;
            }

            try {
                rec.setValue(row, OrderRecCol.MIN_MONTH_AFTER_RECHARGE.index, minMonthAfterRecharge);
                // Write the progress marker last so uncertain writes go to manual review.
                rec.setValue(row, OrderRecCol.RECHARGE_APPLIED.index, 1);
            } catch (Exception e) {
                logger.error("Google Play recharge progress marker failed uid={}",
                        player.getInt(PLAYER_PROPERTY_UID), e);
                return GrantResult.UNKNOWN;
            }
        } else {
            minMonthAfterRecharge = rec.getInt(row, OrderRecCol.MIN_MONTH_AFTER_RECHARGE.index);
        }
        // AntiIndulgenceModule may cap monthly recharge; correct it before final verification.
        if (player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT) < minMonthAfterRecharge) {
            try {
                player.setProperty(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT,
                        minMonthAfterRecharge);
            } catch (Exception e) {
                logger.error("Google Play monthly recharge correction failed uid={} expectedAtLeast={}",
                        player.getInt(PLAYER_PROPERTY_UID), minMonthAfterRecharge, e);
            }
        }
        if (player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT) < minMonthAfterRecharge) {
            return GrantResult.NO_EFFECT;
        }

        try {
            player.setProperty(PLAYER_PROPERTY_ALREADYRECHARGED, true);
        } catch (Exception e) {
            logger.error("Google Play AlreadyRecharged listener failed uid={}",
                    player.getInt(PLAYER_PROPERTY_UID), e);
        }
        return player.getBool(PLAYER_PROPERTY_ALREADYRECHARGED)
                ? GrantResult.GRANTED : GrantResult.NO_EFFECT;
    }

    private void recoverCompletedOrder(IKernel kernel, IGameObject player, int reqid,
                                       JSONObject resp, String packageName,
                                       GooglePlayProduct product, String purchaseToken,
                                       String tokenHash, String processingOwner, IRecord rec) {
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        kernel.flushGooglePlayPlayer(player, flushed -> {
            if (!Boolean.TRUE.equals(flushed)) {
                if (!isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                    abandonLocalLock(tokenHash, processingOwner);
                    return;
                }
                resp.put("retryable", true);
                respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                        "player_save_pending", tokenHash, processingOwner);
                return;
            }
            flushRecoveredOrder(kernel, player, reqid, resp, packageName, product,
                    purchaseToken, tokenHash, processingOwner, rec, true);
        });
    }

    private void flushRecoveredOrder(IKernel kernel, IGameObject player, int reqid,
                                     JSONObject resp, String packageName,
                                     GooglePlayProduct product, String purchaseToken,
                                     String tokenHash, String processingOwner, IRecord rec,
                                     boolean duplicate) {
        int row = findLocalRow(rec, tokenHash);
        if (row == -1 || rec.getInt(row, OrderRecCol.STATUS.index) < PAY_STATUS_AWARDED
                || !localSnapshotMatches(rec, row, product)) {
            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                    "payment_record_missing", tokenHash, processingOwner);
            return;
        }

        int globalStatus = rec.getInt(row, OrderRecCol.CONSUMED.index) == PAY_CONSUMED
                ? GooglePlayOrder.STATUS_CONSUMED : GooglePlayOrder.STATUS_SHIPPED;
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        GooglePlayOrder order = buildGlobalOrder(tokenHash, uid, packageName, product,
                rec.getString(row, OrderRecCol.GOOGLE_ORDER_ID.index), expectedAccountId(uid),
                0L, 1, globalStatus, null);
        kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "completeFromPlayerRecord",
                Collections.singletonList(order), result -> {
                    try {
                        GooglePlayOrder saved = isBlank(result)
                                ? null : JSON.parseObject(result, GooglePlayOrder.class);
                        if (!sameGlobalOrder(saved, tokenHash, uid, packageName, product)
                                || !isCompletedGlobalStatus(saved.getStatus())) {
                            if (!isCurrentRequest(kernel, player, uid,
                                    tokenHash, processingOwner)) {
                                abandonLocalLock(tokenHash, processingOwner);
                                return;
                            }
                            resp.put("retryable", true);
                            respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                    "delivery_record_pending", tokenHash, processingOwner);
                            return;
                        }

                        boolean currentPlayer = isCurrentRequest(kernel, player, uid,
                                tokenHash, processingOwner);
                        if (currentPlayer) {
                            int shippedRow = findLocalRow(rec, tokenHash);
                            if (shippedRow != -1) {
                                rec.setValue(shippedRow, OrderRecCol.STATUS.index, PAY_STATUS_SHIPPED);
                                rec.setValue(shippedRow, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
                            }
                        }
                        if (saved.getStatus() == GooglePlayOrder.STATUS_CONSUMED) {
                            if (currentPlayer) {
                                int consumedRow = findLocalRow(rec, tokenHash);
                                if (consumedRow != -1) {
                                    rec.setValue(consumedRow, OrderRecCol.CONSUMED.index, PAY_CONSUMED);
                                }
                                finishSuccess(kernel, player, reqid, resp, product,
                                        saved.getGoogleOrderId(), duplicate, false,
                                        tokenHash, processingOwner);
                            } else {
                                abandonLocalLock(tokenHash, processingOwner);
                            }
                        } else {
                            consumeAndFinish(kernel, player, reqid, resp, packageName, product,
                                    purchaseToken, tokenHash, processingOwner, uid, duplicate,
                                    saved.getGoogleOrderId(), currentPlayer ? rec : null);
                        }
                    } catch (Exception e) {
                        logger.error("completeFromPlayerRecord error token={}",
                                mask(purchaseToken), e);
                        respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                "server_error", tokenHash, processingOwner);
                    }
                });
    }

    private void markGlobalShippedAndFinish(IKernel kernel, IGameObject player, int reqid,
                                            JSONObject resp, String packageName,
                                            GooglePlayProduct product, String purchaseToken,
                                            String tokenHash, String processingOwner, int uid,
                                            boolean duplicate) {
        kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "markShipped",
                Arrays.asList(tokenHash, uid, processingOwner), result -> {
                    if (!parseBoolean(result)) {
                        IRecord persistedRec = player.getRecord(GOOGLE_PLAY_ORDER_REC);
                        int persistedRow = findLocalRow(persistedRec, tokenHash);
                        if (persistedRow != -1
                                && persistedRec.getInt(persistedRow, OrderRecCol.STATUS.index) >= PAY_STATUS_AWARDED) {
                            flushRecoveredOrder(kernel, player, reqid, resp, packageName,
                                    product, purchaseToken, tokenHash, processingOwner,
                                    persistedRec, duplicate);
                            return;
                        }
                        if (!isCurrentRequest(kernel, player, uid,
                                tokenHash, processingOwner)) {
                            abandonLocalLock(tokenHash, processingOwner);
                            return;
                        }
                        resp.put("retryable", true);
                        respond(kernel, player, reqid, resp, ServerCodeDef.CODE_FAILED,
                                "delivery_record_pending", tokenHash, processingOwner);
                        return;
                    }

                    boolean currentPlayer = isCurrentRequest(kernel, player, uid,
                            tokenHash, processingOwner);
                    IRecord rec = currentPlayer ? player.getRecord(GOOGLE_PLAY_ORDER_REC) : null;
                    int shippedRow = findLocalRow(rec, tokenHash);
                    if (shippedRow != -1) {
                        rec.setValue(shippedRow, OrderRecCol.STATUS.index, PAY_STATUS_SHIPPED);
                        rec.setValue(shippedRow, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
                    }
                    String googleOrderId = shippedRow == -1
                            ? "" : rec.getString(shippedRow, OrderRecCol.GOOGLE_ORDER_ID.index);
                    consumeAndFinish(kernel, player, reqid, resp, packageName, product,
                            purchaseToken, tokenHash, processingOwner, uid, duplicate,
                            googleOrderId, rec);
                });
    }

    private void consumeAndFinish(IKernel kernel, IGameObject player, int reqid,
                                  JSONObject resp, String packageName,
                                  GooglePlayProduct product, String purchaseToken,
                                  String tokenHash, String processingOwner, int uid,
                                  boolean duplicate, String googleOrderId, IRecord rec) {
        try {
            googlePlayIo.execute(() -> {
                boolean consumed = false;
                try {
                    googleVerifier.consumeProductPurchase(
                            packageName, product.productId, purchaseToken);
                    consumed = true;
                } catch (Exception consumeError) {
                    logger.warn("consume Google Play purchase failed productId={} token={}",
                            product.productId, mask(purchaseToken), consumeError);
                    try {
                        GooglePlayPurchaseVerifier.Purchase latest =
                                googleVerifier.getProductPurchase(
                                        packageName, product.productId, purchaseToken);
                        consumed = latest.consumptionState == PAY_CONSUMED;
                    } catch (Exception verifyError) {
                        logger.warn("recheck consumed purchase failed productId={} token={}",
                                product.productId, mask(purchaseToken), verifyError);
                    }
                }

                final boolean consumeSucceeded = consumed;
                kernel.runOnLogicThread(() -> {
                    boolean currentPlayer = isCurrentRequest(kernel, player, uid,
                            tokenHash, processingOwner);
                    if (consumeSucceeded) {
                        if (currentPlayer && rec != null) {
                            int row = findLocalRow(rec, tokenHash);
                            if (row != -1) {
                                rec.setValue(row, OrderRecCol.CONSUMED.index, PAY_CONSUMED);
                                rec.setValue(row, OrderRecCol.UPDATE_TIME.index, kernel.getServerTime());
                            }
                        }
                        kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "markConsumed",
                                Arrays.asList(tokenHash, uid), null);
                    }
                    if (currentPlayer) {
                        finishSuccess(kernel, player, reqid, resp, product, googleOrderId,
                                duplicate, !consumeSucceeded, tokenHash, processingOwner);
                    } else {
                        abandonLocalLock(tokenHash, processingOwner);
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            logger.error("schedule Google Play consume failed token={}", mask(purchaseToken), e);
            if (isCurrentRequest(kernel, player, uid, tokenHash, processingOwner)) {
                finishSuccess(kernel, player, reqid, resp, product, googleOrderId,
                        duplicate, true, tokenHash, processingOwner);
            } else {
                abandonLocalLock(tokenHash, processingOwner);
            }
        }
    }

    private void finishSuccess(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                               GooglePlayProduct product, String googleOrderId,
                               boolean duplicate, boolean consumePending,
                               String tokenHash, String processingOwner) {
        resp.put("productId", product.productId);
        resp.put("orderId", nullToEmpty(googleOrderId));
        resp.put("duplicate", duplicate);
        resp.put("consumePending", consumePending);
        resp.put("balanceProperty", PLAYER_MONEY);
        resp.put("rewardVersion", product.rewardVersion);
        resp.put("rewardGold", product.goldAmount);
        resp.put("gold", player.getLong(PLAYER_MONEY));
        resp.put("headId", product.headId);
        respond(kernel, player, reqid, resp, ServerCodeDef.CODE_SUCCESS,
                duplicate ? "already_fulfilled" : "fulfilled",
                tokenHash, processingOwner);
    }

    private void releaseGlobalForRetry(IKernel kernel, String tokenHash,
                                       int uid, String processingOwner) {
        kernel.executeGooglePlayToStore(GooglePlayOrderService.class, "releaseForRetry",
                Arrays.asList(tokenHash, uid, processingOwner), null);
    }

    private void loadGooglePlayProducts(IKernel kernel) {
        try {
            ICfgReader cfg = kernel.loadXmlConfig(GOOGLE_PLAY_PRODUCTS_PATH);
            if (cfg == null) {
                logger.error("load {} failed; keeping previous Google Play products",
                        GOOGLE_PLAY_PRODUCTS_PATH);
                return;
            }

            Map<String, GooglePlayProduct> loaded = new LinkedHashMap<>();
            for (int i = 0; i < cfg.getItemCount(); i++) {
                String productId = trim(cfg.getString(i, "ProductId"));
                int priceYuan = cfg.getInt(i, "PriceYuan");
                boolean enabled = cfg.getBool(i, "Enabled");
                int rewardVersion = cfg.getInt(i, "RewardVersion");
                long goldAmount = cfg.getLong(i, "Gold");
                int headId = cfg.getInt(i, "HeadId");
                int headHours = cfg.getInt(i, "HeadHours");

                if (!validProductId(productId) || priceYuan <= 0 || rewardVersion <= 0
                        || goldAmount <= 0 || headId < 0 || headHours < 0
                        || (headId == 0 && headHours != 0)
                        || (headId > 0 && headHours <= 0)) {
                    throw new IllegalArgumentException("invalid Google Play product row=" + i
                            + ", productId=" + productId);
                }

                GooglePlayProduct product = new GooglePlayProduct(productId, priceYuan,
                        enabled, rewardVersion, goldAmount, headId, headHours);
                if (loaded.put(productId, product) != null) {
                    throw new IllegalArgumentException("duplicate Google Play ProductId: "
                            + productId);
                }
            }
            if (loaded.isEmpty()) {
                throw new IllegalStateException("GooglePlayProducts.xml has no products");
            }
            googlePlayProducts = Collections.unmodifiableMap(loaded);
            logger.info("loaded independent Google Play products: {}", loaded.keySet());
        } catch (Exception e) {
            logger.error("load Google Play products failed; keeping previous product map", e);
        }
    }

    private int saveVerifiedLocalOrder(IRecord rec, String tokenHash,
                                       GooglePlayProduct product,
                                       String googleOrderId, long now) {
        if (rec == null) {
            return -1;
        }
        int row = findLocalRow(rec, tokenHash);
        if (row == -1) {
            if (!makeRoomForLocalOrder(rec)
                    || !rec.addRow(tokenHash, PAY_STATUS_VERIFIED, product.productId,
                    nullToEmpty(googleOrderId), now, 0, 0,
                    product.rewardVersion, product.priceYuan, product.goldAmount,
                    product.headId, product.headHours, product.fulfillmentHash,
                    0, 0, 0)) {
                return -1;
            }
            return findLocalRow(rec, tokenHash);
        }

        if (!localSnapshotMatches(rec, row, product)) {
            return -1;
        }
        // Status and reward progress only move forward; repeat verification must not downgrade shipped orders.
        if (isBlank(rec.getString(row, OrderRecCol.GOOGLE_ORDER_ID.index)) && !isBlank(googleOrderId)) {
            rec.setValue(row, OrderRecCol.GOOGLE_ORDER_ID.index, googleOrderId);
        }
        rec.setValue(row, OrderRecCol.UPDATE_TIME.index, now);
        return row;
    }

    private int saveCompletedLocalOrder(IRecord rec, String tokenHash,
                                        GooglePlayProduct product, String googleOrderId,
                                        long now, int status, int consumed) {
        int row = saveVerifiedLocalOrder(rec, tokenHash, product, googleOrderId, now);
        if (row != -1) {
            rec.setValue(row, OrderRecCol.STATUS.index, Math.max(rec.getInt(row, OrderRecCol.STATUS.index), status));
            rec.setValue(row, OrderRecCol.REWARD_STEP.index,
                    Math.max(rec.getInt(row, OrderRecCol.REWARD_STEP.index), product.rewardSteps()));
            rec.setValue(row, OrderRecCol.CONSUMED.index,
                    Math.max(rec.getInt(row, OrderRecCol.CONSUMED.index), consumed));
            rec.setValue(row, OrderRecCol.RECHARGE_APPLIED.index, 1);
            rec.setValue(row, OrderRecCol.UPDATE_TIME.index, now);
        }
        return row;
    }

    private boolean makeRoomForLocalOrder(IRecord rec) {
        if (rec.getRows() < rec.getMaxRow()) {
            return true;
        }
        int candidate = -1;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < rec.getRows(); i++) {
            if (rec.getInt(i, OrderRecCol.STATUS.index) >= PAY_STATUS_SHIPPED
                    && rec.getLong(i, OrderRecCol.UPDATE_TIME.index) < oldest) {
                candidate = i;
                oldest = rec.getLong(i, OrderRecCol.UPDATE_TIME.index);
                if (rec.getInt(i, OrderRecCol.CONSUMED.index) == PAY_CONSUMED) {
                    break;
                }
            }
        }
        if (candidate == -1) {
            return false;
        }
        rec.removeRow(candidate);
        return true;
    }

    private int findLocalRow(IRecord rec, String tokenHash) {
        if (rec == null || isBlank(tokenHash)) {
            return -1;
        }
        return rec.findRow(0, OrderRecCol.TOKEN_HASH.index, tokenHash);
    }

    private GooglePlayProduct productFromLocalRecord(IRecord rec, int row) {
        try {
            GooglePlayProduct product = new GooglePlayProduct(
                    rec.getString(row, OrderRecCol.PRODUCT_ID.index),
                    rec.getInt(row, OrderRecCol.PRICE_YUAN.index), true,
                    rec.getInt(row, OrderRecCol.REWARD_VERSION.index),
                    rec.getLong(row, OrderRecCol.GOLD_AMOUNT.index),
                    rec.getInt(row, OrderRecCol.HEAD_ID.index),
                    rec.getInt(row, OrderRecCol.HEAD_HOURS.index));
            return product.fulfillmentHash.equals(rec.getString(row, OrderRecCol.FULFILLMENT_HASH.index))
                    ? product : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean localSnapshotMatches(IRecord rec, int row, GooglePlayProduct product) {
        GooglePlayProduct local = productFromLocalRecord(rec, row);
        return local != null && local.sameFulfillment(product);
    }

    /** 闂佺绻堥崝宀勬儑椤掑倹濯奸柕蹇曞Т缁€瀣叓閸ヨ泛浜為柛娆忛叄瀵即顢涘☉娆戠К婵犵數鍋樼欢銈囨闁秴瀚夋い鎺嗗亾婵犫偓鐎甸晲鐒婃慨姗嗗幗瀵挸霉濠х姴鎳忛弲绋棵归悩渚敽缂傚秴绉瑰顐﹀醇閻旇桨鍑介梺鍝勫暢濞夋稖銇愰懠顑藉亾閻熼偊妲搁柛娆忕箲缁岄亶顢欑粵瀣Η闂佺粯鍩堥崢钘壝洪崹顕呯叾闁稿本绨介埡鍛?*/
    private boolean reconcileUntouchedLocalSnapshot(IRecord rec, int row,
                                                    GooglePlayProduct product) {
        if (localSnapshotMatches(rec, row, product)) {
            return true;
        }
        if (rec == null || row < 0
                || rec.getInt(row, OrderRecCol.STATUS.index) > PAY_STATUS_VERIFIED
                || rec.getInt(row, OrderRecCol.REWARD_STEP.index) != 0
                || rec.getInt(row, OrderRecCol.RECHARGE_APPLIED.index) != 0) {
            return false;
        }
        rec.setValue(row, OrderRecCol.PRODUCT_ID.index, product.productId);
        rec.setValue(row, OrderRecCol.REWARD_VERSION.index, product.rewardVersion);
        rec.setValue(row, OrderRecCol.PRICE_YUAN.index, product.priceYuan);
        rec.setValue(row, OrderRecCol.GOLD_AMOUNT.index, product.goldAmount);
        rec.setValue(row, OrderRecCol.HEAD_ID.index, product.headId);
        rec.setValue(row, OrderRecCol.HEAD_HOURS.index, product.headHours);
        rec.setValue(row, OrderRecCol.FULFILLMENT_HASH.index, product.fulfillmentHash);
        return localSnapshotMatches(rec, row, product);
    }

    private static GooglePlayProduct productFromGlobalOrder(GooglePlayOrder order) {
        if (order == null || order.getProductId() == null || order.getPriceYuan() == null
                || order.getRewardVersion() == null || order.getGoldAmount() == null
                || order.getHeadId() == null || order.getHeadHours() == null
                || isBlank(order.getFulfillmentHash())) {
            return null;
        }
        try {
            GooglePlayProduct product = new GooglePlayProduct(order.getProductId(),
                    order.getPriceYuan(), true, order.getRewardVersion(),
                    order.getGoldAmount(), order.getHeadId(), order.getHeadHours());
            return product.fulfillmentHash.equals(order.getFulfillmentHash()) ? product : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static GooglePlayOrder buildGlobalOrder(String tokenHash, int uid,
                                                     String packageName,
                                                     GooglePlayProduct product,
                                                     String googleOrderId,
                                                     String accountId,
                                                     long purchaseTimeMillis,
                                                     int quantity, int status,
                                                     String processingOwner) {
        GooglePlayOrder order = new GooglePlayOrder();
        order.setPurchaseTokenHash(tokenHash);
        order.setUid(uid);
        order.setPackageName(packageName);
        order.setProductId(product.productId);
        order.setGoogleOrderId(nullToEmpty(googleOrderId));
        order.setObfuscatedAccountId(accountId);
        order.setPurchaseTimeMillis(purchaseTimeMillis);
        order.setQuantity(quantity);
        order.setPriceYuan(product.priceYuan);
        order.setRewardVersion(product.rewardVersion);
        order.setGoldAmount(product.goldAmount);
        order.setHeadId(product.headId);
        order.setHeadHours(product.headHours);
        order.setFulfillmentHash(product.fulfillmentHash);
        order.setStatus(status);
        order.setProcessingOwner(processingOwner);
        return order;
    }

    private static boolean sameGlobalOrder(GooglePlayOrder order, String tokenHash,
                                           int uid, String packageName,
                                           GooglePlayProduct product) {
        if (order == null || !tokenHash.equals(order.getPurchaseTokenHash())
                || order.getUid() == null || order.getUid() != uid
                || !packageName.equals(order.getPackageName())) {
            return false;
        }
        GooglePlayProduct stored = productFromGlobalOrder(order);
        return stored != null && stored.sameFulfillment(product);
    }

    private static boolean sameGlobalIdentity(GooglePlayOrder order, String tokenHash,
                                              int uid, String packageName,
                                              String productId) {
        return order != null && tokenHash.equals(order.getPurchaseTokenHash())
                && order.getUid() != null && order.getUid() == uid
                && packageName.equals(order.getPackageName())
                && productId.equals(order.getProductId());
    }

    private static boolean isOwnedProcessingOrder(GooglePlayOrder order, String owner) {
        return order != null && order.getStatus() != null
                && order.getStatus() == GooglePlayOrder.STATUS_PROCESSING
                && owner.equals(order.getProcessingOwner());
    }

    private static boolean isCompletedGlobalStatus(Integer status) {
        return status != null && (status == GooglePlayOrder.STATUS_SHIPPED
                || status == GooglePlayOrder.STATUS_CONSUMED);
    }

    private boolean acquireLocalLock(String tokenHash, String owner) {
        long now = System.nanoTime();
        LocalProcessingLock next = new LocalProcessingLock(owner, now);
        while (true) {
            LocalProcessingLock current = localProcessing.putIfAbsent(tokenHash, next);
            if (current == null) {
                return true;
            }
            if (now - current.createdAtNanos
                    < TimeUnit.MILLISECONDS.toNanos(LOCAL_LOCK_LEASE_MS)
                    || leaseHeartbeats.containsKey(tokenHash)) {
                return false;
            }
            if (localProcessing.replace(tokenHash, current, next)) {
                return true;
            }
        }
    }

    private boolean acquireFulfillment(int uid, String owner) {
        String existing = fulfillmentByUid.putIfAbsent(uid, owner);
        if (existing != null && !owner.equals(existing)) {
            return false;
        }
        fulfillmentUidByOwner.put(owner, uid);
        return true;
    }

    private void releaseFulfillment(String owner) {
        Integer uid = fulfillmentUidByOwner.remove(owner);
        if (uid != null) {
            fulfillmentByUid.remove(uid, owner);
        }
    }

    private boolean tryAcquireVerification(int uid) {
        final boolean[] acquired = {false};
        verifyingByUid.compute(uid, (key, counter) -> {
            AtomicInteger value = counter == null ? new AtomicInteger() : counter;
            if (value.get() < MAX_VERIFY_IN_FLIGHT_PER_UID) {
                value.incrementAndGet();
                acquired[0] = true;
            }
            return value;
        });
        return acquired[0];
    }

    private void releaseVerification(int uid) {
        verifyingByUid.computeIfPresent(uid, (key, counter) ->
                counter.decrementAndGet() <= 0 ? null : counter);
    }

    private boolean startLeaseHeartbeat(IKernel kernel, String tokenHash,
                                        int uid, String processingOwner) {
        try {
            AtomicBoolean requestInFlight = new AtomicBoolean();
            AtomicBoolean valid = new AtomicBoolean(true);
            AtomicLong lastAckAt = new AtomicLong(System.nanoTime());
            ScheduledFuture<?> future = leaseHeartbeatExecutor.scheduleAtFixedRate(() ->
                            kernel.runOnLogicThread(() -> {
                                if (!ownsLocalLock(tokenHash, processingOwner)) {
                                    stopLeaseHeartbeat(tokenHash, processingOwner);
                                    return;
                                }
                                if (!requestInFlight.compareAndSet(false, true)) {
                                    return;
                                }
                                long requestNanos = System.nanoTime();
                                kernel.executeGooglePlayToStore(GooglePlayOrderService.class,
                                        "renewLease", Arrays.asList(tokenHash, uid,
                                                processingOwner), renewed -> {
                                            requestInFlight.set(false);
                                            long elapsedNanos = System.nanoTime() - requestNanos;
                                            boolean staleAck = elapsedNanos
                                                    > TimeUnit.MILLISECONDS.toNanos(
                                                    LEASE_ACK_FRESH_MS);
                                            if (parseLong(renewed) <= 0 || staleAck
                                                    || !ownsLocalLock(tokenHash,
                                                    processingOwner)) {
                                                valid.set(false);
                                                logger.warn("Google Play lease heartbeat failed/stale uid={} token={} elapsedMs={}",
                                                        uid, shortHash(tokenHash),
                                                        TimeUnit.NANOSECONDS.toMillis(
                                                                elapsedNanos));
                                            } else {
                                                lastAckAt.set(System.nanoTime());
                                            }
                                        });
                            }),
                    LEASE_HEARTBEAT_SECONDS, LEASE_HEARTBEAT_SECONDS,
                    TimeUnit.SECONDS);
            LeaseHeartbeat next = new LeaseHeartbeat(processingOwner, future,
                    valid, lastAckAt);
            LeaseHeartbeat previous = leaseHeartbeats.put(tokenHash, next);
            if (previous != null) {
                previous.future.cancel(false);
            }
            return true;
        } catch (RejectedExecutionException e) {
            logger.warn("schedule Google Play lease heartbeat failed uid={}", uid, e);
            return false;
        }
    }

    private boolean ownsLocalLock(String tokenHash, String processingOwner) {
        LocalProcessingLock lock = localProcessing.get(tokenHash);
        return lock != null && processingOwner.equals(lock.owner);
    }

    private boolean hasValidLeaseHeartbeat(String tokenHash, String processingOwner) {
        LeaseHeartbeat heartbeat = leaseHeartbeats.get(tokenHash);
        return heartbeat != null && processingOwner.equals(heartbeat.owner)
                && heartbeat.valid.get()
                && System.nanoTime() - heartbeat.lastAckAt.get()
                <= TimeUnit.MILLISECONDS.toNanos(LEASE_HEARTBEAT_STALE_MS);
    }

    private void stopLeaseHeartbeat(String tokenHash, String processingOwner) {
        LeaseHeartbeat heartbeat = leaseHeartbeats.get(tokenHash);
        if (heartbeat != null && processingOwner.equals(heartbeat.owner)
                && leaseHeartbeats.remove(tokenHash, heartbeat)) {
            heartbeat.future.cancel(false);
        }
    }

    private static boolean isCurrentPlayer(IKernel kernel, IGameObject player, int uid) {
        return uid > 0 && kernel.getPlayer(uid) == player;
    }

    private boolean isCurrentRequest(IKernel kernel, IGameObject player, int uid,
                                     String tokenHash, String processingOwner) {
        return isCurrentPlayer(kernel, player, uid)
                && ownsLocalLock(tokenHash, processingOwner);
    }

    private void abandonLocalLock(String tokenHash, String processingOwner) {
        if (isBlank(processingOwner)) {
            return;
        }
        if (!isBlank(tokenHash)) {
            LocalProcessingLock lock = localProcessing.get(tokenHash);
            if (lock != null && processingOwner.equals(lock.owner)) {
                localProcessing.remove(tokenHash, lock);
            }
            stopLeaseHeartbeat(tokenHash, processingOwner);
        }
        releaseFulfillment(processingOwner);
    }

    private void respond(IKernel kernel, IGameObject player, int reqid, JSONObject resp,
                         ServerCodeDef code, String status,
                         String tokenHash, String processingOwner) {
        try {
            resp.put("code", code.getId());
            resp.put("status", status);
            if ("processing".equals(status)) {
                resp.put("retryable", true);
            }
            // Log the response after the state machine has reached this outcome.
            // Never log the raw purchase token or the entire client request.
            logger.info("Google Play response uid={} reqid={} verificationId={} productId={} orderId={} "
                            + "code={} status={} retryable={} manualReview={} duplicate={} consumePending={} "
                            + "balanceProperty={} balance={}",
                    player == null ? 0 : player.getInt(PLAYER_PROPERTY_UID), reqid,
                    resp.getString("verificationId"), resp.getString("productId"), resp.getString("orderId"),
                    code.getId(), status, resp.getBooleanValue("retryable"), resp.getBooleanValue("manualReview"),
                    resp.getBooleanValue("duplicate"), resp.getBooleanValue("consumePending"),
                    resp.getString("balanceProperty"), resp.getLong("gold"));
            kernel.response(player, reqid, CustomMsg.String.newBuilder()
                    .setValue(resp.toJSONString()).build().toByteArray());
        } catch (Exception e) {
            logger.error("respond Google Play purchase failed reqid={}", reqid, e);
        } finally {
            abandonLocalLock(tokenHash, processingOwner);
        }
    }

    /** 闁哄鏅滈弻銊ッ?null 闁荤偞绋忛崝搴ㄥΦ濮樿泛绀傚ù锝囩摂閸熷懘鎮归幇闈涘箳闁圭兘浜堕弫宥囦沪閼恒儱鍓ㄩ梺鍛婂笚閻熴劎鎹㈤幋锕€鐐婇柣鎰閺冨倵鍋撶憴鍕殤婵炲牊鍨垮畷婊冾吋閸ユ湹绱撻梺缁橆焾閸╂牠鍩€椤戣法顦﹂柣鈯欏懐绠旈柨鏃囧劵椤╊偊鏌?*/
    private String checkPurchaseEligibility(IGameObject player, int priceYuan) {
        if (isGooglePlayTestPayPlayer(player)) {
            return null;
        }
        if (!getBooleanConfig("googlePlayEnforceMinorLimits", true)) {
            return null;
        }
        if (!player.getBool(PLAYER_PROPERTY_CERTIFICATION)) {
            return "real_name_required";
        }
        int age = player.getInt(PLAYER_PROPERTY_AGE);
        if (age < 8) {
            return "age_restricted";
        }
        int monthCharge = player.getInt(PLAYER_PROPERTY_MONTHRECHARGEAMOUNT);
        if (age < 16 && (priceYuan > 50 || (long) monthCharge + priceYuan > 200)) {
            return "minor_payment_limit";
        }
        if (age < 18 && (priceYuan > 100 || (long) monthCharge + priceYuan > 400)) {
            return "minor_payment_limit";
        }
        return null;
    }

    private static boolean isGooglePlayTestPayPlayer(IGameObject player) {
        return getBooleanConfig("testPay", false)
                && player != null
                && player.getBool(PLAYER_PROPERTY_TESTPAY);
    }

    private static boolean requireObfuscatedAccountId() {
        return getBooleanConfig("googlePlayRequireObfuscatedAccountId", true);
    }

    private static boolean getBooleanConfig(String key, boolean defaultValue) {
        Object configured = SystemConfigData.getConfig(key, (Object) defaultValue);
        return configured instanceof Boolean
                ? (Boolean) configured
                : Boolean.parseBoolean(String.valueOf(configured));
    }

    private static String getConfiguredPackageName() {
        return trim(SystemConfigData.getConfig("googlePlayPackageName", ""));
    }

    private static String getAccountIdSecret() {
        return trim(SystemConfigData.getConfig("googlePlayAccountIdSecret", ""));
    }

    private static boolean hasValidAccountIdSecret() {
        return getAccountIdSecret().length() >= 32;
    }

    private static String expectedAccountId(int uid) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(getAccountIdSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return toHex(mac.doFinal((ACCOUNT_ID_PREFIX + uid)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static String fulfillmentHash(String productId, int priceYuan,
                                          int rewardVersion, long goldAmount,
                                          int headId, int headHours) {
        return sha256Hex(productId + "|" + priceYuan + "|" + rewardVersion
                + "|" + goldAmount + "|" + headId + "|" + headHours);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >>> 4) & 0x0f, 16));
            hex.append(Character.forDigit(b & 0x0f, 16));
        }
        return hex.toString();
    }

    private static int saturatedAdd(int value, int delta) {
        long result = (long) value + delta;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static boolean parseBoolean(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(trim(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static boolean validProductId(String value) {
        return !isBlank(value) && value.length() <= 200
                && value.matches("[a-z0-9._]+");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String mask(String value) {
        if (isBlank(value)) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****"
                + value.substring(value.length() - 4);
    }

    private static String shortHash(String value) {
        return isBlank(value) ? "" : value.substring(0, Math.min(8, value.length()));
    }

    private static final class GooglePlayProduct {
        private final String productId;
        private final int priceYuan;
        private final boolean enabled;
        private final int rewardVersion;
        private final long goldAmount;
        private final int headId;
        private final int headHours;
        private final String fulfillmentHash;

        private GooglePlayProduct(String productId, int priceYuan, boolean enabled,
                                  int rewardVersion, long goldAmount,
                                  int headId, int headHours) {
            if (!validProductId(productId) || priceYuan <= 0 || rewardVersion <= 0
                    || goldAmount <= 0 || headId < 0 || headHours < 0
                    || (headId == 0 && headHours != 0)
                    || (headId > 0 && headHours <= 0)) {
                throw new IllegalArgumentException("invalid Google Play product snapshot: "
                        + productId);
            }
            this.productId = productId;
            this.priceYuan = priceYuan;
            this.enabled = enabled;
            this.rewardVersion = rewardVersion;
            this.goldAmount = goldAmount;
            this.headId = headId;
            this.headHours = headHours;
            this.fulfillmentHash = fulfillmentHash(productId, priceYuan,
                    rewardVersion, goldAmount, headId, headHours);
        }

        private int rewardSteps() {
            return headId > 0 ? 2 : 1;
        }

        private boolean sameFulfillment(GooglePlayProduct other) {
            return other != null && productId.equals(other.productId)
                    && fulfillmentHash.equals(other.fulfillmentHash);
        }
    }

    private enum GrantResult {
        GRANTED,
        NO_EFFECT,
        UNKNOWN
    }

    private static final class LocalProcessingLock {
        private final String owner;
        private final long createdAtNanos;

        private LocalProcessingLock(String owner, long createdAtNanos) {
            this.owner = owner;
            this.createdAtNanos = createdAtNanos;
        }
    }

    private static final class LeaseHeartbeat {
        private final String owner;
        private final ScheduledFuture<?> future;
        private final AtomicBoolean valid;
        private final AtomicLong lastAckAt;

        private LeaseHeartbeat(String owner, ScheduledFuture<?> future,
                               AtomicBoolean valid, AtomicLong lastAckAt) {
            this.owner = owner;
            this.future = future;
            this.valid = valid;
            this.lastAckAt = lastAckAt;
        }
    }
}
