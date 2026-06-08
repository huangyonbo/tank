//package game.modules.utils;
//
//import com.google.common.base.Preconditions;
//
//
//import java.util.List;
//import java.util.Objects;
//import java.util.SortedMap;
//import java.util.TreeMap;
//
///**
// *
// * @param <K> ID
// * @param <V> 权重值
// */
//public class WeightRandom<K, V extends Number> {
//
//    private final TreeMap<Double, K> weightMap = new TreeMap<Double, K>();
//
//    public WeightRandom(List<Pair<K, V>> list) {
//        Objects.requireNonNull(list, "list can NOT be null!");
//        for (Pair<K, V> pair : list) {
//            Preconditions.checkArgument(pair.getValue().doubleValue() >= 0, String.format("非法权重值：pair=%s", pair));
//            // 统一转为double
//            double lastWeight = this.weightMap.size() == 0 ? 0 : this.weightMap.lastKey();
//            // 权重累加
//            this.weightMap.put(pair.getValue().doubleValue() + lastWeight, pair.getKey());
//        }
//    }
//
//    public K random() {
//        double randomWeight = this.weightMap.lastKey() * Math.random();
//        SortedMap<Double, K> tailMap = this.weightMap.tailMap(randomWeight, false);
//        return this.weightMap.get(tailMap.firstKey());
//    }
//}
