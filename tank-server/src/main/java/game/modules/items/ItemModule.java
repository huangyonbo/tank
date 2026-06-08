/**
 * 描述：   道具模块
 */
package game.modules.items;

import back.modules.MailModule;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.PropertyKey;
import framework.game.*;
import framework.game.IKernel.PlayerLogType;
import game.constant.OfflineDataType;
import game.custommsg.*;
import game.modules.OfflineDataModule;
import game.modules.activities.ActivityMgr;
import game.modules.player.BagModule;
import game.modules.statemachine.StateMachine.State;
import game.modules.utils.ItemTipType;
import game.modules.utils.UtilFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * 描述：
 *
 */
public class ItemModule implements ILogicModule {

    enum eUseCondition {
        CANT, ALL_CAN, ONLY_IN_GAME, IN_GAME_NOT_GROUP, // 非鱼阵状态

        END
    }

    private static Logger logger = LoggerFactory.getLogger(ItemModule.class);

    private BagModule m_BagModule;
    private MailModule m_MailModule;
    private OfflineDataModule m_OfflineDataModule;
    private ActivityMgr m_activityMgr;
    private PropertyItem propertyItem;
    private String[] m_CardType = {"", "中国移动", "中国联通", "中国电信"};

    public ItemModule(IKernel kernel) {
        kernel.addClass("BaseItem", "Item"); // 基本道具
    }

    private Map<String, Integer> m_mapItemScore = new HashMap<>();

    /**
     * @param kernel
     * @return
     */
    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regClientMessage(C2SMsgDef.C2S_USE_ITEM.ordinal(), this, "OnUseItem");
        kernel.regClientMessage(C2SMsgDef.C2S_READ_ITEM.ordinal(), this, "OnReadItem");
        kernel.regRequestMessage(RequestMsgDef.REQ_ITEMRECYCLE.ordinal(), this, "OnRecycleItem");


        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerCreateClass");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Item", this, "OnItemCreateClass");
        kernel.regEvent(KernelEvent.KEVENT_ON_CLASS_READY, "Item", this, "OnItemClassReady");
        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");

        kernel.preLoadConfig("res/Items/BaseItem.xml");
        kernel.preLoadConfig("res/Items/RealItem.xml");
        m_BagModule = (BagModule) kernel.getModule("BagModule");
        m_MailModule = (MailModule) kernel.getModule("MailModule");
        m_OfflineDataModule = (OfflineDataModule) kernel.getModule("OfflineDataModule");
        m_activityMgr = (ActivityMgr) kernel.getModule("ActivityMgr");
        propertyItem = (PropertyItem) kernel.getModule("PropertyItem");
//        RefreshCfg(kernel, "res/ItemScore/ItemScore.xml");
//        if (m_BagModule == null || m_MailModule == null
//                || m_OfflineDataModule == null || m_activityMgr == null || propertyItem == null) {
//            return false;
//        }
        return true;
    }

    void RefreshCfg(IKernel kernel, String path) {
        if (path.equals("res/ItemScore/ItemScore.xml")) {
            ICfgReader cfg = kernel.loadXmlConfig(path);
            if (cfg == null) {
                return;
            }
            int count = cfg.getItemCount();
            for (int i = 0; i < count; ++i) {
                m_mapItemScore.put(cfg.getString(i, "Id"), cfg.getInt(i, "Score"));
            }
        }
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
    }

    public void OnItemCreateClass(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_ISNEW, ValueType.BOOL, false, true, true);
        kernel.declareProperty(script, "CanSend", ValueType.BOOL, false, true, false);
        kernel.declareProperty(script, "Recycle", ValueType.BOOL, false, true, false);
        kernel.declareProperty(script, "RecyclePrice", ValueType.STRING, false, true, false);
        kernel.declareProperty(script, "UseCondition", ValueType.INT, false, false, false);
        kernel.declareProperty(script, "Cost", ValueType.FLOAT, false, false, false);
        kernel.declareProperty(script, "CheckList", ValueType.BOOL, false, false, false);
        kernel.declareProperty(script, "SendCount", ValueType.STRING, false, true, false);
        kernel.declareProperty(script, "SendTimes", ValueType.STRING, false, true, false);
        kernel.declareProperty(script, "SocietyWarehouse", ValueType.BOOL, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_ITEMSCORE, ValueType.INT, false, false, false);
    }

    public void OnPlayerCreateClass(IKernel kernel, String script) {
        // 当天赠送记录
        IRecord rec = kernel.declareRecord(script, "SendRecord", 3, 100, false, false, true);
        rec.setColType(0, ValueType.INT); //玩家uid
        rec.setColType(1, ValueType.INT); //所有道具赠送数和
        rec.setColType(2, ValueType.LONG); //数据时间戳
    }

    public void OnItemClassReady(IKernel kernel, String script) {
        kernel.setVisible(script, "Id", false, true, false);
        kernel.setVisible(script, "Count", false, true, true);
        kernel.setVisible(script, "EndTime", false, true, true);
    }

    boolean UseItem(IKernel kernel, IGameObject player, IGameObject item, int count, int system, String reason) {
        if (count <= 0) {
            return false;
        }

        int useCondition = item.getInt("UseCondition");
        if (useCondition == eUseCondition.CANT.ordinal()) {
            return false;
        } else if (useCondition == eUseCondition.ONLY_IN_GAME.ordinal()) {
            IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
            if (desk == null) {
                return false;
            }
        } else if (useCondition == eUseCondition.IN_GAME_NOT_GROUP.ordinal()) {
            IGameObject desk = kernel.getGameObject(player.getLong(PLAYER_PROPERTY_DESKID));
            if (desk == null) {
                return false;
            }
            // 鱼阵时不能使用限制道具
            if (desk.getInt("State") == State.STATE_GROUP.ordinal()) {
                return false;
            }
        }

        // 兑换券类道具
        boolean isCardItem = item.getScript().equals("CardItem");
        if (isCardItem) {
            count = 1;
        }

        int haveCount = item.getInt("Count");
        if (haveCount < count) {
            logger.info("OnUseItem haveCount < count {} {}", haveCount, count);
            return false;
        }

        if (item.getBool("CheckList")) {
            int score = (int) (item.getFloat("Cost") * count);
            int _score = player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) - score;
            _score = _score < 0 ? 0 : _score;
            player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE, _score);
            player.setProperty(PLAYER_PROPERTY_USEBOMBSCORE, player.getInt(PLAYER_PROPERTY_USEBOMBSCORE) + score);
        }

        int leftCount = haveCount - count;
        StringBuilder sb = new StringBuilder();
        sb.append(haveCount).append(",").append(-count).append(",").append(leftCount);

        item.setProperty("Count", leftCount);

        if (!isCardItem) {
            kernel.command(item, CommandDef.CMD_USE_ITEM.ordinal(), player, count, system, reason);
        }

        //kernel.addGameLog(player, LogKind.OTHERS, LogType.USED, item.getString("Id") + "*" + count, getClass().getName(), "UseItem", -1);
        kernel.addPlayerLog(player, item, PlayerLogType.DEL_ITEM.ordinal(), system, sb.toString(), reason);

        String itemid = item.getString("Id");
        String itemname = item.getString(PLAYER_PROPERTY_NAME);

        if (isCardItem) {
            int type = 0;
            if (item.haveTempData("CardType")) {
                type = item.getTempInt("CardType");
                item.removeTempData("CardType");
            }
            int cardType = type;

            int uid = player.getInt(PLAYER_PROPERTY_UID);
            long objid = player.getObjectID();
            // 注册时间在48小时以内不给兑换话费卡 add by 赵俊@2019/10/16 13:13
//			long regTime = 0;
//			try {
//				regTime = UtilFunc.DateParse(player.GetString(PLAYER_PROPERTY_REGTIME));
//			} catch (ParseException e) {
//				logger.error("RegTime error");
//			} finally {
//				if (kernel.GetServerTime() <= regTime + 172800000) {
//					// 使用XX失败，请联系客服
//					UtilFunc.ShowTip(kernel, player, "TXT_USE_CARD_FAILED", itemname);
//					// 补发一张新卡
//					AddItem(kernel, player, itemid, count, UtilFunc.System.CARD_ITEM.ordinal(), "Use card failed");
//					return false;
//				}
//			}
            kernel.checkCardItem(player, itemid, cardType, (List<IKernel.CardData> cardData) -> {
                if (cardData == null) {
                    if (kernel.getGameObject(objid) == player) {
                        // 使用XX失败，请联系客服
                        UtilFunc.showTip(kernel, player, "TXT_USE_CARD_FAILED", itemname);
                        // 补发一张新卡
                        AddItem(kernel, player, itemid, 1, UtilFunc.System.CARD_ITEM.ordinal(), "Use card failed");
                    } else {
                        // 添加离线数据
                        m_OfflineDataModule.AddOfflineData(kernel, uid, OfflineDataType.USE_CARD_FAILED, itemid,
                                "Use card failed");
                    }
                } else {
                    if (kernel.getGameObject(objid) == player) {
                        // 使用XX成功
                        UtilFunc.showTip(kernel, player, "TXT_USE_CARD_SUCCESS", itemname);
                    }
                }
            });
        }

        if (leftCount == 0) {
            kernel.destroyGameObject(item);
        }
        return true;
    }

    // 使用道具
    void OnUseItem(IKernel kernel, IGameObject player, int msgid, byte[] msg) throws InvalidProtocolBufferException {
        CustomMsg.UseItem useItem = CustomMsg.UseItem.parseFrom(msg);
        int pos = useItem.getPos();
        int count = useItem.getCount();
        if (count <= 0) {
            return;
        }

        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            logger.info("OnUseItem itemBag not found");
            return;
        }

        IGameObject item = itemBag.getChild(pos);
        if (item == null) {
            logger.info("OnUseItem item == null");
            return;
        }

        boolean useSuccess = UseItem(kernel, player, item, count, UtilFunc.System.BAG.ordinal(), "Client use item");
    }

    // 使用道具
    public boolean UseItem(IKernel kernel, IGameObject player, String itemid, int count, int system, String reason) {
        if (count <= 0) {
            return false;
        }

        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            logger.info("OnUseItem itemBag not found");
            return false;
        }

        int pos = itemBag.findChildById(0, itemid);
        if (pos == -1) {
            return false;
        }

        IGameObject item = itemBag.getChild(pos);
        if (item == null) {
            logger.info("OnUseItem item == null");
            return false;
        }
        return UseItem(kernel, player, item, count, system, reason);
    }

    public void OnReadItem(IKernel kernel, IGameObject player, int msgid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.ReadItem read = CustomMsg.ReadItem.parseFrom(msg);
        int pos = read.getPos();

        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return;
        }

        IGameObject item = itemBag.getChild(pos);
        if (item == null) {
            return;
        }
        item.setProperty(PLAYER_PROPERTY_ISNEW, false);
    }

    void OnRecycleItem(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) throws InvalidProtocolBufferException {
        CustomMsg.SendItem sendItem = CustomMsg.SendItem.parseFrom(msg);
        int pos = sendItem.getPos();
        int count = sendItem.getCount();
        CustomMsg.ServerCode.Builder code = CustomMsg.ServerCode.newBuilder();
        if (count < 0 || pos < 0) {
            code.setCode(ServerCodeDef.CODE_COUNT_ILLEGAL.ordinal());
            return;
        }
        IGameObject item = GetItem(player, pos);
        if (item == null) {
            code.setCode(ServerCodeDef.CODE_NOT_EXIST.ordinal());
            return;
        } else if (!item.getBool("Recycle")) {
            code.setCode(ServerCodeDef.CODE_CANT_RECYCLE.ordinal());
            return;
        } else if (count > item.getInt("Count")) {
            code.setCode(ServerCodeDef.CODE_NEED_ITEM.ordinal());
            return;
        }
        String itemId = item.getString("Id");
        String recyclePrice = item.getString("RecyclePrice");
        SubItem(kernel, player, pos, count, UtilFunc.System.RECYCLE_ITEM.ordinal(), "Recycle item: " + itemId);
        //ItemLogModule.AddItemLog(kernel, player, itemId, count, ItemLogEnum.ITEM_RECYCLE.ordinal());
        long addGold = Long.parseLong(recyclePrice) * count;
        player.setProperty(PLAYER_PROPERTY_GOLD, player.getLong(PLAYER_PROPERTY_GOLD) + addGold);
        code.setCode(ServerCodeDef.CODE_SUCCESS.ordinal());
        UtilFunc.sendItemTips(kernel, player, ItemTipType.TIP_GET_SUCCESS, "item_gold_1", (int) addGold);
        kernel.response(player, reqid, code.build().toByteArray());
    }

    private static final int LIMIT = 20_0000_0000;

    /**
     * 因货币范围数值为long，暂是修改添加方式为拆分添加
     */
    public boolean AddItem(IKernel kernel, IGameObject player, String itemName, long count, int system, String reason) {
        long time = count / LIMIT;
        boolean result = true;
        for (int i = 0; i < time; i++) {
            result = AddItem(kernel, player, itemName, LIMIT, system, reason);
            if (!result){
                logger.info("AddItem error uid:{},itemName:{},count:{},system:{},reason:{}",player.getInt(PropertyKey.PLAYER_PROPERTY_UID),itemName,LIMIT,system,reason);
            }
        }
        int residue = (int) (count % LIMIT);
        result = AddItem(kernel, player, itemName, residue, system, reason);
        if (!result){
            logger.info("AddItem error uid:{},itemName:{},count:{},system:{},reason:{}",player.getInt(PropertyKey.PLAYER_PROPERTY_UID),itemName,result,system,reason);
        }
        return result;
    }

    // 添加道具
    public boolean AddItem(IKernel kernel, IGameObject player, String itemName, int count, int system, String reason) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return false;
        }
        if (itemName.equals("item_debris_GoldenStorm")) {
            player.setProperty(PLAYER_PROPERTY_TOTALDEBRISGOLDENSTORM, player.getInt(PLAYER_PROPERTY_TOTALDEBRISGOLDENSTORM) + count);
        }
        if (itemName.equals("item_DoubleEleven_coin")) {
            player.setProperty(PLAYER_PROPERTY_11_COIN, player.getInt(PLAYER_PROPERTY_11_COIN) + count);
        }
        int pos = itemBag.findChildById(0, itemName);
        if (pos != -1) {
            IGameObject item = itemBag.getChild(pos);
            if (item.getInt("LifeTime") == -1) {
                // 无时效，可堆叠
                int oldCount = item.getInt("Count");
                item.setProperty("Count", item.getInt("Count") + count);
                item.setProperty(PLAYER_PROPERTY_ISNEW, true);
                // 弹药转单头的时候不计算分数
                if (item.getBool("CheckList")/* && !"ammo transform to hbomb".equals(reason)*/) {// ty
                    //保留代码永不执行  2022.7.13
                    if (false){
                        player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE,
                                player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) + ((int) item.getFloat("Cost") * count));
                    }
                    if (item.getString("Id").equals("item_skill_nbomb")){
                        int item_skill_nbomb = this.GetItemCount(kernel, player, "item_skill_nbomb");
                        player.setProperty(PLAYER_PROPERTY_HAVENBOMB_SCORE,item_skill_nbomb);
                    }else if (item.getString("Id").equals("item_skill_hbomb")){
                        int item_skill_hbomb = this.GetItemCount(kernel, player, "item_skill_hbomb");
                        player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE,item_skill_hbomb);
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append(oldCount).append(",").append(count).append(",").append(item.getInt("Count"));
                kernel.addPlayerLog(player, item, PlayerLogType.GET_ITEM.ordinal(), system, sb.toString(), reason);
                kernel.command(item, CommandDef.CMD_GET_ITEM.ordinal(), player, count, system, reason);
                // 自动使用 pkg（ItemPkg.xml）
                if (!reason.contains("Use itempkg")) {
                    addItemLog(kernel, player, itemName, count, system, reason);
                }
                try {
                    if (item.getString("Id").equals("item_skill_hbomb")){
                        player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE,item.getInt("Count"));
                    }
                } catch (Exception e) {
                }
                return true;
            }
        }

        IGameObject item = kernel.createObjectByConfig(itemName, null);
        if (item == null) {
            return false;
        }

        if (item.getBool("AutoUse")) {
            StringBuilder sb = new StringBuilder();
            sb.append(0).append(",").append(count).append(",").append(count);
            kernel.addPlayerLog(player, item, PlayerLogType.GET_ITEM.ordinal(), system, sb.toString(), reason + "(AutoUse)");
            kernel.command(item, CommandDef.CMD_USE_ITEM.ordinal(), player, count, system, reason);
            sb.delete(0, sb.length());
            sb.append(count).append(",").append(-count).append(",").append(0);
            kernel.addPlayerLog(player, item, PlayerLogType.DEL_ITEM.ordinal(), system, sb.toString(), reason + "(AutoUse)");
            if (item.getBool("CheckList")) {
                int score = (int) item.getFloat("Cost") * count;
                player.setProperty(PLAYER_PROPERTY_USEBOMBSCORE, player.getInt(PLAYER_PROPERTY_USEBOMBSCORE) + score);

            }
            kernel.destroyGameObject(item);
        } else {
            item.setProperty("Count", count);
            item.setProperty(PLAYER_PROPERTY_ISNEW, true);
            pos = itemBag.addChild(item);
            if (pos == -1) {
                kernel.destroyGameObject(item);
                return false;
            }

            if (item.getBool("CheckList")) {
                //保留代码永不执行  2022.7.13
                if (false){
                    player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE,
                            player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) + ((int) item.getFloat("Cost") * count));
                }
                if (item.getString("Id").equals("item_skill_nbomb")){
                    int item_skill_nbomb = this.GetItemCount(kernel, player, "item_skill_nbomb");
                    player.setProperty(PLAYER_PROPERTY_HAVENBOMB_SCORE,item_skill_nbomb);
                }else if (item.getString("Id").equals("item_skill_hbomb")){
                    int item_skill_hbomb = this.GetItemCount(kernel, player, "item_skill_hbomb");
                    player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE,item_skill_hbomb);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(0).append(",").append(count).append(",").append(count);
            try {
                if (item.getString("Id").equals("item_skill_hbomb")){
                    int item_skill_hbomb = this.GetItemCount(kernel, player, "item_skill_hbomb");
                    player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE,item_skill_hbomb);
                }
            } catch (Exception e) {
            }
            kernel.addPlayerLog(player, item, PlayerLogType.GET_ITEM.ordinal(), system, sb.toString(),
                    reason + "(New)");
            m_BagModule.OnCheckItemLife(kernel, item);
        }
        kernel.command(item, CommandDef.CMD_GET_ITEM.ordinal(), player, count, system, reason);
        // 自动使用 pkg（ItemPkg.xml）
        if (!reason.contains("Use itempkg")) {
            addItemLog(kernel, player, itemName, count, system, reason);
        }
        return true;
    }

    public void addItemLog(IKernel kernel, IGameObject player, String itemName, int count, int system, String reason) {
        String outPut = UtilFunc.System.getLabel(system);
        // 击杀鱼掉落的道具增加鱼的信息
        if (system == UtilFunc.System.KILL_FISH.ordinal()){
            String[] strArr = reason.split(" ");
            outPut += strArr[strArr.length - 1];
        }
        ItemLogModule.AddItemLog(kernel, player, itemName, count, outPut + "获得", "-");
    }

    /**
     * 因货币范围数值为long，暂是修改添加方式为拆分扣除
     */
    public int SubItem(IKernel kernel, IGameObject player, String itemName, long count, int system, String reason) {
        long time = count / LIMIT;
        int result;
        for (int i = 0; i < time; i++) {
            result = SubItem(kernel, player, itemName, LIMIT, system, reason);
            if (result != LIMIT) {
                logger.info("SubItem error uid:{},itemName:{},count:{},system:{},reason:{}", player.getInt(PropertyKey.PLAYER_PROPERTY_UID), itemName, LIMIT, system, reason);
            }
        }
        int residue = (int) (count % LIMIT);
        result = SubItem(kernel, player, itemName, residue, system, reason);
        if (result != residue) {
            logger.info("SubItem error uid:{},itemName:{},count:{},system:{},reason:{}", player.getInt(PropertyKey.PLAYER_PROPERTY_UID), itemName, result, system, reason);
        }
        return result;
    }

    public int SubItem(IKernel kernel, IGameObject player, String itemName, int count, int system, String reason) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);

        if (itemBag == null) {
            logger.error("SubItem itemBag == null");
            return 0;
        }
        int needCount = count;
        int subCount = 0;
        int pos = -1;
        while (needCount > 0) {
            pos = itemBag.findChildById(++pos, itemName);
            if (pos == -1) {
                IGameObject item = kernel.createObjectByConfig(itemName, null);
                if (item != null && item.getScript().equals("PropertyItem")) {

                    kernel.command(item, CommandDef.CMD_SUB_PROPERTY_ITEM.ordinal(), player, count, system, reason);
                    if (item.getBool("CheckList")) {
                        int score = player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) - (int) item.getFloat("Cost") * count;
                        score = Math.max(score, 0);
                        player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE, score);
                    }
                    ItemLogModule.AddItemLog(kernel, player, itemName, count, "-", UtilFunc.System.getLabel(system) + "消耗");
                    kernel.destroyGameObject(item);
                    return count;
                }
                logger.error("SubItem pos == -1  {}",itemName);
                return subCount;
            }

            count = needCount;
            IGameObject item = itemBag.getChild(pos);
            int have = item.getInt("Count");
            if (have < count) {
                count = have;
            }

            int left = have - count;
            StringBuilder sb = new StringBuilder();
            sb.append(have).append(",").append(-count).append(",").append(left);

            if (item.getBool("CheckList")) {
                int score = player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) - (int) item.getFloat("Cost") * count;
                score = Math.max(score, 0);
                player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE, score);
            }

            kernel.addPlayerLog(player, item, PlayerLogType.DEL_ITEM.ordinal(), system, sb.toString(), reason);
            if (left == 0) {
                kernel.destroyGameObject(item);
            } else {
                item.setProperty("Count", left);
            }
            subCount += count;
            needCount -= count;
            if (item.getString("Id").equals("item_skill_nbomb")){
                int item_skill_nbomb = this.GetItemCount(kernel, player, "item_skill_nbomb");
                player.setProperty(PLAYER_PROPERTY_HAVENBOMB_SCORE,item_skill_nbomb);
            }else if (item.getString("Id").equals("item_skill_hbomb")){
                int item_skill_hbomb = this.GetItemCount(kernel, player, "item_skill_hbomb");
                player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE,item_skill_hbomb);
            }
            if (itemName.equals("item_skill_hbomb")) {
                player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE, left);
            }
        }

        ItemLogModule.AddItemLog(kernel, player, itemName, count, "-", UtilFunc.System.getLabel(system) + "消耗");
        return subCount;
    }

    public int SubItem(IKernel kernel, IGameObject player, int pos, int count, int system, String reason) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return 0;
        }
        IGameObject item = itemBag.getChild(pos);
        if (item == null) {
            return 0;
        }
        String itemName = item.getString("Id");

        int have = item.getInt("Count");
        if (have < count) {
            count = have;
        }
        int left = have - count;

        StringBuilder sb = new StringBuilder();
        sb.append(have).append(",").append(-count).append(",").append(left);

        if (item.getBool("CheckList")) {
            int score = player.getInt(PLAYER_PROPERTY_HAVEBOMBSCORE) - (int) item.getFloat("Cost") * count;
            score = score < 0 ? 0 : score;
            player.setProperty(PLAYER_PROPERTY_HAVEBOMBSCORE, score);
        }
        kernel.addPlayerLog(player, item, PlayerLogType.DEL_ITEM.ordinal(), system, sb.toString(), reason);
        if (itemName.equals("item_skill_hbomb")) {
            player.setProperty(PLAYER_PROPERTY_HAVEHBOMB_SCORE, left);
        }
        if (left == 0) {
            kernel.destroyGameObject(item);
        } else {
            item.setProperty("Count", left);
        }

        return count;
    }

    //获取道具对象 ById
    public IGameObject GetItem(IGameObject player, String itemId) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return null;
        }
        int pos = itemBag.findChildById(0, itemId);
        if (pos == -1) {
            return null;
        }
        return itemBag.getChild(pos);
    }

    //获取道具对象 By位置
    public IGameObject GetItem(IGameObject player, int pos) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return null;
        }
        return itemBag.getChild(pos);
    }

    //获取道具数量
    public int GetItemCount(IKernel kernel, IGameObject player, String itemName) {
        IGameObject itemBag = player.getContainer(BagModule.ITEM_BAG);
        if (itemBag == null) {
            return 0;
        }
        int count = 0;
        int pos = itemBag.findChildById(0, itemName);
        while (pos != -1) {
            IGameObject item = itemBag.getChild(pos);
            count += item.getInt("Count");
            pos = itemBag.findChildById(pos + 1, itemName);
        }
        return count;
    }

    /**
     * 获取属性道具数量
     */
    public Long GetPropertyItemCount(IKernel kernel, IGameObject player, String itemId) {
        XmlPropertyItem xmlPropertyItem = propertyItem.getPropertyItemMap().get(itemId);
        if (xmlPropertyItem != null) {
            String property = xmlPropertyItem.getProperty();
            return (Long) player.getProperty(property);
        }
        return 0L;
    }

    public int getDoubleElevenCoin(IKernel kernel) {
        return Integer.parseInt(kernel.getCfgProperty("item_DoubleEleven_coin","DailyLimit"));
    }



}
