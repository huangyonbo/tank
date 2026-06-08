package framework.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author HYB
 * @Date 2025/11/6
 * @Time 11:57
 * @Desc
 */
public class SlidingWindowRateLimiter {
    private final int windowSize;// 窗口划分份数
    private final long windowLengthInMs;// 总滑动窗口时间(毫秒)
    private final long bucketLengthInMs;// 每个小窗口的时间
    private final int limit;// 最大允许请求数
    private final AtomicInteger[] counters;// 每个窗口的计数器
    private final long[] windowTimestamps; // 每个小窗口的起始时间（ms）

    public SlidingWindowRateLimiter(int windowSize, long windowLengthInMs, int limit) {
        if (windowLengthInMs <= 0 || windowSize <= 0) {
            throw new IllegalArgumentException("windowLengthInMs and bucketCount must be > 0");
        }
        this.windowSize = windowSize;
        this.windowLengthInMs = windowLengthInMs;
        this.bucketLengthInMs = windowLengthInMs / windowSize;
        this.limit = limit;
        this.counters = new AtomicInteger[windowSize];
        this.windowTimestamps = new long[windowSize];
        for (int i = 0; i < windowSize; i++) {
            counters[i] = new AtomicInteger(0);
            windowTimestamps[i] = 0L;
        }
    }

    /**
     * 尝试获取一次许可（线程安全）
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        int currentIndex = (int) ((now / bucketLengthInMs) % windowSize);
        long bucketStart = now - (now % bucketLengthInMs);
        // 如果当前桶是旧的（时间戳不同），重置当前桶计数并设置时间戳
        if (windowTimestamps[currentIndex] != bucketStart) {
            counters[currentIndex].set(0);
            windowTimestamps[currentIndex] = bucketStart;
        }
        // 统计窗口内总请求数：只统计属于有效窗口的桶
        int total = 0;
        for (int i = 0; i < windowSize; i++) {
            long ts = windowTimestamps[i];
            if (ts == 0) continue;
            if (now - ts < windowLengthInMs) {
                total += counters[i].get();
            }
        }
        if (total >= limit) {
            return false; // 超限
        }
        // 当前桶 +1
        counters[currentIndex].incrementAndGet();
        return true;
    }
}
