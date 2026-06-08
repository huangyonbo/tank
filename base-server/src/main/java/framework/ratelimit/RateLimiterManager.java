package framework.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author hyb
 * @Date 2025/11/6
 * @Time 11:58
 * @Desc
 */
@Component
public class RateLimiterManager {
    private final static int WINDOW_SIZE = 10;// 窗口划分份数
    private final static long WINDOW_LENGTH_IN_MS = 1000;// 总滑动窗口时间(毫秒)
    private int limit = 500;//在总滑动窗口限制数量
    private int errorLimit = 100;

    private Cache<Long, SlidingWindowRateLimiter> limiterCache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .maximumSize(10_0000)
                    .build();

    private Cache<Long, SlidingWindowRateLimiter> errorLimitCache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .maximumSize(10_0000)
                    .build();

    private SlidingWindowRateLimiter getLimiter(long uid) {
        try {
            return limiterCache.get(uid, () -> {
                return new SlidingWindowRateLimiter(WINDOW_SIZE, WINDOW_LENGTH_IN_MS, limit);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to create limiter for uid=" + uid, e);
        }
    }

    private SlidingWindowRateLimiter getErrorLimiter(long uid) {
        try {
            return errorLimitCache.get(uid, () -> {
                return new SlidingWindowRateLimiter(WINDOW_SIZE, 60000, errorLimit);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to create error limiter for uid=" + uid, e);
        }
    }

    private boolean tryErrorAcquire(long uid) {
        return getErrorLimiter(uid).tryAcquire();
    }

    public boolean tryAcquire(long uid) {
        boolean result = getLimiter(uid).tryAcquire();
        if (!result) {
            return tryErrorAcquire(uid);
        }
        return result;
    }

    public void setLimit(int limit, int errorLimit) {
        this.limit = limit;
        this.errorLimit = errorLimit;
    }
}
