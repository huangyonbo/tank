package framework.mybatis.service.impl;

import framework.mybatis.domain.GooglePlayOrder;
import framework.mybatis.mapper.GooglePlayOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Google Play 订单的全服唯一占用与短租约发货状态机。 */
@Service
public class GooglePlayOrderService extends AbstractServiceImpl<GooglePlayOrderMapper, GooglePlayOrder> {
    private static final long PROCESSING_LEASE_MS = 5L * 60 * 1000;

    @Transactional
    public GooglePlayOrder claimAndAcquire(GooglePlayOrder request) {
        if (!isValidRequest(request) || isBlank(request.getProcessingOwner())) {
            return null;
        }

        String processingOwner = request.getProcessingOwner();
        long now = dbNow();
        request.setGoogleOrderId(blankToNull(request.getGoogleOrderId()));
        request.setStatus(GooglePlayOrder.STATUS_VERIFIED);
        request.setProcessingOwner(null);
        request.setCreateTime(now);
        request.setUpdateTime(now);
        baseMapper.insertIfAbsent(request);
        baseMapper.backfillPurchaseMetadata(request.getPurchaseTokenHash(), request.getUid(),
                request.getPackageName(), request.getProductId(), request.getGoogleOrderId(),
                request.getObfuscatedAccountId(), request.getPurchaseTimeMillis(),
                request.getQuantity());

        GooglePlayOrder saved = findClaimedOrder(request);
        if (!samePurchaseIdentity(saved, request)) {
            return saved;
        }

        baseMapper.acquire(request.getPurchaseTokenHash(), request.getUid(), processingOwner,
                now, now - PROCESSING_LEASE_MS);
        return queryByTokenHash(request.getPurchaseTokenHash());
    }

    public boolean releaseForRetry(String tokenHash, Integer uid, String processingOwner) {
        if (isBlank(tokenHash) || uid == null || isBlank(processingOwner)) {
            return false;
        }
        return baseMapper.release(tokenHash, uid, processingOwner, dbNow()) > 0;
    }

    /** 发货前续租并校验租约仍有效；过期处理者不能重新续租。 */
    public long renewLease(String tokenHash, Integer uid, String processingOwner) {
        if (isBlank(tokenHash) || uid == null || isBlank(processingOwner)) {
            return 0L;
        }
        long now = dbNow();
        return baseMapper.renewLease(tokenHash, uid, processingOwner,
                now, now - PROCESSING_LEASE_MS) > 0 ? now : 0L;
    }

    public boolean markShipped(String tokenHash, Integer uid, String processingOwner) {
        if (isBlank(tokenHash) || uid == null || isBlank(processingOwner)) {
            return false;
        }
        long now = dbNow();
        return baseMapper.markShipped(tokenHash, uid, processingOwner,
                now, now - PROCESSING_LEASE_MS) > 0;
    }

    public boolean markManualReview(String tokenHash, Integer uid) {
        if (!isLowerHexSha256(tokenHash) || uid == null || uid <= 0) {
            return false;
        }
        GooglePlayOrder order = queryByTokenHash(tokenHash);
        if (order != null && uid.equals(order.getUid())
                && Integer.valueOf(GooglePlayOrder.STATUS_MANUAL_REVIEW).equals(order.getStatus())) {
            return true;
        }
        return baseMapper.markManualReview(tokenHash, uid, dbNow()) > 0;
    }

    public boolean markConsumed(String tokenHash, Integer uid) {
        if (isBlank(tokenHash) || uid == null) {
            return false;
        }
        GooglePlayOrder order = queryByTokenHash(tokenHash);
        if (order != null && order.getStatus() != null
                && order.getStatus() == GooglePlayOrder.STATUS_CONSUMED
                && uid.equals(order.getUid())) {
            return true;
        }
        return baseMapper.markConsumed(tokenHash, uid, dbNow()) > 0;
    }

    /** 用玩家持久化记录恢复“已发货/已消费”状态，也用于从旧版本平滑迁移。 */
    @Transactional
    public GooglePlayOrder completeFromPlayerRecord(GooglePlayOrder request) {
        if (!isValidRequest(request) || request.getStatus() == null
                || (request.getStatus() != GooglePlayOrder.STATUS_SHIPPED
                && request.getStatus() != GooglePlayOrder.STATUS_CONSUMED)) {
            return null;
        }
        long now = dbNow();
        request.setGoogleOrderId(blankToNull(request.getGoogleOrderId()));
        request.setProcessingOwner(null);
        request.setCreateTime(now);
        request.setUpdateTime(now);
        baseMapper.insertIfAbsent(request);
        int promoted = baseMapper.promoteCompleted(request.getPurchaseTokenHash(), request.getUid(),
                request.getPackageName(), request.getProductId(), request.getGoogleOrderId(),
                request.getObfuscatedAccountId(), request.getPurchaseTimeMillis(), request.getQuantity(),
                request.getPriceYuan(),
                request.getRewardVersion(), request.getGoldAmount(), request.getHeadId(),
                request.getHeadHours(), request.getFulfillmentHash(), request.getStatus(), now);
        GooglePlayOrder saved = findClaimedOrder(request);
        return (promoted > 0 || sameCompletedOrder(saved, request)) ? saved : null;
    }

    private GooglePlayOrder findClaimedOrder(GooglePlayOrder request) {
        GooglePlayOrder saved = queryByTokenHash(request.getPurchaseTokenHash());
        if (saved == null && !isBlank(request.getGoogleOrderId())) {
            saved = lambdaQuery().eq(GooglePlayOrder::getGoogleOrderId, request.getGoogleOrderId()).one();
        }
        return saved;
    }

    private GooglePlayOrder queryByTokenHash(String tokenHash) {
        return lambdaQuery().eq(GooglePlayOrder::getPurchaseTokenHash, tokenHash).one();
    }

    private static boolean samePurchaseIdentity(GooglePlayOrder saved, GooglePlayOrder request) {
        if (saved == null || !request.getPurchaseTokenHash().equals(saved.getPurchaseTokenHash())
                || !request.getUid().equals(saved.getUid())
                || !request.getPackageName().equals(saved.getPackageName())
                || !request.getProductId().equals(saved.getProductId())) {
            return false;
        }
        if (!isBlank(request.getGoogleOrderId()) && !isBlank(saved.getGoogleOrderId())
                && !request.getGoogleOrderId().equals(saved.getGoogleOrderId())) {
            return false;
        }
        if (!request.getObfuscatedAccountId().equals(saved.getObfuscatedAccountId())) {
            return false;
        }
        if (!request.getQuantity().equals(saved.getQuantity())) {
            return false;
        }
        return request.getPurchaseTimeMillis() == 0L || saved.getPurchaseTimeMillis() == null
                || saved.getPurchaseTimeMillis() == 0L
                || request.getPurchaseTimeMillis().equals(saved.getPurchaseTimeMillis());
    }

    private static boolean sameCompletedOrder(GooglePlayOrder saved, GooglePlayOrder request) {
        if (!samePurchaseIdentity(saved, request)
                || !request.getPriceYuan().equals(saved.getPriceYuan())
                || !request.getRewardVersion().equals(saved.getRewardVersion())
                || !request.getGoldAmount().equals(saved.getGoldAmount())
                || !request.getHeadId().equals(saved.getHeadId())
                || !request.getHeadHours().equals(saved.getHeadHours())
                || !request.getFulfillmentHash().equals(saved.getFulfillmentHash())) {
            return false;
        }
        return saved.getStatus() != null
                && (saved.getStatus().equals(request.getStatus())
                || (request.getStatus() == GooglePlayOrder.STATUS_SHIPPED
                && saved.getStatus() == GooglePlayOrder.STATUS_CONSUMED));
    }

    private static boolean isValidRequest(GooglePlayOrder request) {
        return request != null && isLowerHexSha256(request.getPurchaseTokenHash())
                && request.getUid() != null && request.getUid() > 0
                && validLength(request.getPackageName(), 200)
                && validLength(request.getProductId(), 200)
                && (isBlank(request.getGoogleOrderId())
                || validLength(request.getGoogleOrderId(), 100))
                && isLowerHexSha256(request.getObfuscatedAccountId())
                && request.getPurchaseTimeMillis() != null
                && request.getPurchaseTimeMillis() >= 0
                && Integer.valueOf(1).equals(request.getQuantity())
                && request.getPriceYuan() != null && request.getPriceYuan() > 0
                && request.getRewardVersion() != null && request.getRewardVersion() > 0
                && request.getGoldAmount() != null && request.getGoldAmount() > 0
                && request.getHeadId() != null && request.getHeadId() >= 0
                && request.getHeadHours() != null && request.getHeadHours() >= 0
                && ((request.getHeadId() == 0 && request.getHeadHours() == 0)
                || (request.getHeadId() > 0 && request.getHeadHours() > 0))
                && isLowerHexSha256(request.getFulfillmentHash())
                && (isBlank(request.getProcessingOwner())
                || validLength(request.getProcessingOwner(), 64));
    }

    private long dbNow() {
        Long now = baseMapper.currentTimeMillis();
        if (now == null || now <= 0) {
            throw new IllegalStateException("database clock unavailable");
        }
        return now;
    }

    private static boolean validLength(String value, int maxLength) {
        return !isBlank(value) && value.length() <= maxLength;
    }

    private static boolean isLowerHexSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
