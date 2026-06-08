package game.modules.activities;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

/**
 * Alias Method（别名法）
 * O(1) 加权随机，适合高并发 / 实时游戏
 *
 * @Author HYB
 */
public final class AliasRandom<T> {

    private final List<T> items;
    private final double[] prob;
    private final int[] alias;
    private final int size;

    public AliasRandom(List<T> source, ToDoubleFunction<T> weightFn) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(weightFn, "weightFn is null");

        List<T> valid = new ArrayList<>();
        for (T t : source) {
            if (weightFn.applyAsDouble(t) > 0) {
                valid.add(t);
            }
        }

        if (valid.isEmpty()) {
            throw new IllegalArgumentException("No element with positive weight");
        }

        this.items = Collections.unmodifiableList(valid);
        this.size = items.size();
        this.prob = new double[size];
        this.alias = new int[size];

        build(weightFn);
    }

    private void build(ToDoubleFunction<T> weightFn) {
        double sum = 0;
        for (T t : items) {
            sum += weightFn.applyAsDouble(t);
        }
        if (sum <= 0 || Double.isNaN(sum) || Double.isInfinite(sum)) {
            throw new IllegalArgumentException("Invalid weight sum: " + sum);
        }

        double avg = sum / size;
        double[] scaled = new double[size];

        Deque<Integer> small = new ArrayDeque<>();
        Deque<Integer> large = new ArrayDeque<>();

        for (int i = 0; i < size; i++) {
            scaled[i] = weightFn.applyAsDouble(items.get(i)) / avg;
            if (scaled[i] < 1.0) {
                small.add(i);
            } else {
                large.add(i);
            }
        }

        while (!small.isEmpty() && !large.isEmpty()) {
            int s = small.poll();
            int l = large.poll();

            prob[s] = scaled[s];
            alias[s] = l;

            scaled[l] = (scaled[l] + scaled[s]) - 1.0;
            if (scaled[l] < 1.0) {
                small.add(l);
            } else {
                large.add(l);
            }
        }

        while (!large.isEmpty()) {
            int i = large.poll();
            prob[i] = 1.0;
            alias[i] = i;
        }
        while (!small.isEmpty()) {
            int i = small.poll();
            prob[i] = 1.0;
            alias[i] = i;
        }
    }

    /**
     * O(1) 随机
     */
    public T random() {
        int i = ThreadLocalRandom.current().nextInt(size);
        double r = ThreadLocalRandom.current().nextDouble();
        return r < prob[i] ? items.get(i) : items.get(alias[i]);
    }
}
