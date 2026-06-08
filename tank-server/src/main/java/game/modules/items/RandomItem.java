//package game.modules.items;
//
//import framework.MathUtils;
//import framework.game.*;
//import game.custommsg.CommandDef;
//import game.modules.player.VipModule;
//import game.modules.utils.ItemTipType;
//import game.modules.utils.Pair;
//import game.modules.utils.UtilFunc;
//import game.modules.utils.WeightRandom;
//import lombok.Data;
//import org.apache.commons.lang.math.RandomUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//public class RandomItem implements ILogicModule {
//
//    @Data
//    class PkgData {
//        String item;
//        int minCount;
//        int maxCount;
//        int finalCount;
//        int weight;
//
//    }
//
//    @Data
//    class RandPkg {
//        int totalWeight = 0;
//        int type = 0;
//        List<PkgData> list = new ArrayList<>();
//        boolean limitJF;
//    }
//
//    private static Logger logger = LoggerFactory.getLogger(RandomItem.class);
//    Map<String, RandPkg> m_mapPkgs = new HashMap<>();
//    List<Pair<Integer, Integer>> pairs = new ArrayList<>();
//    WeightRandom<Integer, Integer> weightRandom = null;
//
//    private ItemModule m_itemModule = null;
//    private VipModule m_vipModule;
//
//    public RandomItem(IKernel kernel) {
//        kernel.addClass("RandomItem", "Item"); // 随机道具
//    }
//
//    @Override
//    public boolean onInit(IKernel kernel) {
//        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "RandomItem", this, "OnItemClassCreate");
//
//        kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "RandomItem", this, "OnUseItem");
//
//        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
//        RefreshCfg(kernel, "res/Items/RandomItem.xml");
//
//        kernel.preLoadConfig("res/Items/RandomItem.xml");
//
//        m_itemModule = (ItemModule) kernel.getModule("ItemModule");
//        m_vipModule = (VipModule) kernel.getModule("VipModule");
//        return true;
//    }
//
//    @Override
//    public void onDestroy() {
//
//    }
//
//    public Set<String> getRandItemMapKeys() {
//        return m_mapPkgs.keySet();
//    }
//
//    void RefreshCfg(IKernel kernel, String path) {
//        if (path.equals("res/Items/RandomItem.xml")) {
//            m_mapPkgs.clear();
//            LoadConfig(kernel, path);
//        }
//    }
//
//    public boolean LoadConfig(IKernel kernel, String path) {
//        ICfgReader cfg = kernel.loadXmlConfig(path);
//        int count = cfg.getItemCount();
//        pairs.clear();
//        for (int i = 0; i < count; ++i) {
//            String id = cfg.getString(i, "Id");//道具包id
//            String strPkg = cfg.getString(i, "RandomPkg");//随机物品内容
//            int type = cfg.getInt(i, "Type"); // 0-固定概率 1-弹头基础概率 2-礼包固定概率
//            boolean limitJF = cfg.getBool(i, "IntegralImpact"); // 是否受积分限制
//
//            RandPkg pkg = new RandPkg();
//            pkg.type = type;
//            pkg.limitJF = limitJF;//是否受积分限制
//            String[] rands = strPkg.split(";");
//            for (int j = 0; j < rands.length; ++j) {
//                String[] wd = rands[j].split(":");
//                if (wd.length != 2) {
//                    logger.error("RandomItem {}: parse pkg failed: {}", id, strPkg);
//                    continue;
//                }
//
//                String[] its = wd[0].split("\\*");
//                if (its.length != 2) {
//                    logger.error("RandomItem {}: parse pkg failed: {}", id, strPkg);
//                    continue;
//                }
//
//                PkgData data = new PkgData();
//                data.item = its[0];//道具名称
//
//                try {
//                    String[] dur = its[1].split(",");
//                    if (dur.length != 2) {
//                        logger.error("RandomItem {}: parse pkg failed: {}", id, strPkg);
//                        continue;
//                    }
//                    data.minCount = Integer.parseInt(dur[0]);//最少数量,前
//                    data.maxCount = Integer.parseInt(dur[1]);//最大数量,后
//                    data.weight = Integer.parseInt(wd[1]);//权重:后
//                } catch (NumberFormatException e) {
//                    continue;
//                }
//
//                pkg.totalWeight += data.weight;//伪随机 有积分限制
//                pkg.list.add(data);
//
//            }
//            m_mapPkgs.put(id, pkg);
//        }
//        return true;
//    }
//
//    public void OnItemClassCreate(IKernel kernel, String script) {
//    }
//
//    public void OnUseItem(IKernel kernel, IGameObject item, Object... objects) {
//        IGameObject player = (IGameObject) objects[0];
//        int count = (int) objects[1];
//
//        String id = item.getString("Id");
//        if (!m_mapPkgs.containsKey(id)) {
//            return;
//        }
//        RandPkg pkg = m_mapPkgs.get(id);
//
////		long now = kernel.GetServerTime();
////		long lastStone2Time = player.GetLong(PLAYER_PROPERTY_LASTSTONE2TIME);
////		if (UtilFunc.GetZeroTime(now) != UtilFunc.GetZeroTime(lastStone2Time)) {
////			player.SetProperty(PLAYER_PROPERTY_TODAYSTONE2NBOMBAMOUNT, 0);
////			player.SetProperty(PLAYER_PROPERTY_TODAYSTONE2HBOMBAMOUNT, 0);
////		}
////		player.SetProperty(PLAYER_PROPERTY_LASTSTONE2TIME, now);
//
//        if (pkg.limitJF) {
//            for (int i = 0; i < count; ++i) {
//                PkgData data = Random(pkg, kernel, player);
//                String itemName = data.item;
//                String itemScore = kernel.getCfgProperty(itemName, PLAYER_PROPERTY_ITEMSCORE);
//                if (itemScore != null) {
//                    int score = Integer.parseInt(itemScore) * data.finalCount;
//                    player.setProperty(PLAYER_PROPERTY_ITEMSCORE, player.getInt(PLAYER_PROPERTY_ITEMSCORE) + score);
//                }
//                m_itemModule.AddItem(kernel, player, data.item, data.finalCount, UtilFunc.System.RANDOM_ITEM.ordinal(), "Use randomItem " + item.getString("Id"));
////			ItemLogModule.AddItemLog(kernel,player, data.item, data.finalCount, ItemLogEnum.NAVIGATION_ACT_GET.ordinal());
//                UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, data.item, data.finalCount);
//                logger.info("玩家{}使用伪随机礼包{}获得道具{}", player.getInt(PLAYER_PROPERTY_UID), id, data.item + "*" + data.finalCount);
//            }
//        } else {
//            PkgData randomItem = getRandomItem(pkg);
//            String itemName = randomItem.item;
//            int itemCount = randomItem.finalCount;
//            m_itemModule.AddItem(kernel, player, itemName, itemCount, UtilFunc.System.RANDOM_ITEM.ordinal(), "Use randomItem " + item.getString("Id"));
//            UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, itemName, itemCount);
//            logger.info("玩家{}使用真随机礼包{}获得道具{}", player.getInt(PLAYER_PROPERTY_UID), id, itemName + "*" + itemCount);
//        }
//
//    }
//
//    //使用随机礼包真随机
//    private PkgData getRandomItem(RandPkg pkg) {
//        List<PkgData> list = pkg.list;
//        pairs.clear();
//        for (int i = 0; i < list.size(); i++) {
//            pairs.add(new Pair<>(i, list.get(i).weight));
//        }
//        weightRandom = new WeightRandom<>(pairs);
//        Integer id = weightRandom.random();
//        PkgData pkgData = list.get(id);
//        int max = pkgData.maxCount;
//        int min = pkgData.minCount;
//        int cz = max - min;
//        int finalCount = cz>0?RandomUtils.nextInt(cz) + min:min;
//        logger.debug("真随机礼包最大值{},最小值{},差值{},随机值{}",max,min,cz,finalCount);
//        pkgData.finalCount = finalCount;
//        return pkgData;
//    }
//
//    private float calWeight(int amount, int todayAmount, int limit, int baseWeight) {
//        float weight = 0;
//        if (todayAmount >= limit) {
//            weight = 0;
//        } else {
//            if (amount < limit) {
//                weight = baseWeight;
//            } else {
//                BigDecimal amountLimit = new BigDecimal(amount - limit);
//                BigDecimal one = new BigDecimal("1");
//                BigDecimal zeroPointTwo = new BigDecimal("0.2");
//                BigDecimal bBase = new BigDecimal(baseWeight);
//                float realWeight = bBase.multiply(one.subtract(amountLimit.multiply(zeroPointTwo))).floatValue();
//                if (realWeight < 0) {
//                    realWeight = 0;
//                }
//                weight = new BigDecimal(String.valueOf(weight)).add(new BigDecimal(String.valueOf(realWeight)))
//                        .floatValue();
//            }
//        }
//        return weight;
//    }
//
//    PkgData Random(RandPkg pkg, IKernel kernel, IGameObject player) {
//        return null;
//    }
//}
