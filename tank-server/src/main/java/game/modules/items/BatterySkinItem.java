package game.modules.items;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import game.custommsg.C2SMsgDef;
import game.custommsg.CommandDef;
import game.custommsg.CustomMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 描述： 炮台皮肤
 *
 */
public class BatterySkinItem implements ILogicModule {

    private static Logger logger = LoggerFactory.getLogger(BatterySkinItem.class);
    private int m_nBombBattery = 14;
    private Map<Integer, BatteryConfig> configMap = new HashMap<>();

    class BatteryConfig {
        int id;
        float attackSpeed;
        int autoFireTime;
        int autoFireCooling;
        boolean func1Open;
        boolean func2Open;
        int vipLimit;
        int betUpAdd;
        int crit;
        int store;
    }


    public BatterySkinItem(IKernel kernel) {
        kernel.addClass("BatterySkinItem", "Item"); // 炮台皮肤
    }

    public enum UserBatterySkinColEnum {
        ITEM_ID, END_DATE, IN_USE, COL_MAX
    }

    /**
     * @param kernel
     * @return
     */
    @Override
    public boolean onInit(IKernel kernel) {
        // kernel.RegEvet(KernelEvent.KEVENT_ON_CREATE_CLASS, "BatterySkinItem",
        // this,
        // "OnItemClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_LOAD, "Player", this, "OnPlayerLoad");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "Player", this, "OnPlayerClassCreate");
        kernel.regEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, "BatterySkinItem", this, "OnItemClassCreate");

        // 从背包使用
        kernel.regCommand(CommandDef.CMD_USE_ITEM.ordinal(), "BatterySkinItem", this, "OnUseItemInBag");
        //从个人信息里选择使用
        kernel.regClientMessage(C2SMsgDef.C2S_SELECT_BATTERY_SKIN.ordinal(), this, "OnSelectBatterySkin");

        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "refreshCfg");
        kernel.regEvent(KernelEvent.KEVENT_ON_SITDOWN, "Player", this, "OnPlayerSitDown");

        kernel.preLoadConfig("res/Items/BatterySkinItem.xml");
        refreshCfg(kernel, "res/Items/BatterySkinItem.xml");
        kernel.declareHeartBeat("HB_CheckBatterySkin", this, "OnCheckBatterySkin");
        return true;
    }

    void refreshCfg(IKernel kernel, String path) {
        if ("res/Items/BatterySkinItem.xml".equals(path)) {
            configMap.clear();
            ICfgReader cfg = kernel.loadXmlConfig(path);
            if (cfg == null) {
                return;
            }
            int count = cfg.getItemCount();
            for (int i = 0; i < count; ++i) {
                BatteryConfig config = new BatteryConfig();
                config.id = cfg.getInt(i, "SkinID");
                config.attackSpeed = cfg.getFloat(i, "AttackSpeed");
                config.autoFireTime = cfg.getInt(i, "AutoFireTime");
                config.autoFireCooling = cfg.getInt(i, "AutoFireCooling");
                config.func1Open = cfg.getBool(i, "SkillFunc1");
                config.func2Open = cfg.getBool(i, "SkillFunc2");
                config.crit = cfg.getInt(i, "Crit");
                config.betUpAdd = cfg.getInt(i, "BetUpAdd");
                config.vipLimit = cfg.getInt(i, "VipLimit");
                config.store = cfg.getInt(i, "Store");
                configMap.put(config.id, config);
            }
        }
    }

    /**
     *
     */
    @Override
    public void onDestroy() {

    }

    private void updateBatteryProp(IGameObject player, int batteryInUse) {
        BatteryConfig config = configMap.get(batteryInUse);
        if (config != null) {
            player.setProperty(PLAYER_PROPERTY_BATTERY_SPEED, config.attackSpeed);
            player.setProperty(PLAYER_PROPERTY_BATTERY_FUNC1, config.func1Open);
            player.setProperty(PLAYER_PROPERTY_BATTERY_FUNC2, config.func2Open);
            player.setProperty(PLAYER_PROPERTY_BATTERY_AUTO, config.autoFireTime);
            player.setProperty(PLAYER_PROPERTY_BATTERY_AUTO_COOL, config.autoFireCooling);
            player.setProperty(PLAYER_PROPERTY_BATTERY_STORE, config.store);
        } else {
            player.setProperty(PLAYER_PROPERTY_BATTERY_SPEED, 0f);
            player.setProperty(PLAYER_PROPERTY_BATTERY_FUNC1, false);
            player.setProperty(PLAYER_PROPERTY_BATTERY_FUNC2, false);
            player.setProperty(PLAYER_PROPERTY_BATTERY_AUTO, 100);
            player.setProperty(PLAYER_PROPERTY_BATTERY_AUTO_COOL, 120);
            player.setProperty(PLAYER_PROPERTY_BATTERY_STORE, 0);
        }
    }

    void OnPlayerSitDown(IKernel kernel, IGameObject player, IGameObject desk) {
        int batteryInUse = player.getInt(PLAYER_PROPERTY_BATTERYINUSE);
        updateBatteryProp(player, batteryInUse);
    }

    void OnPlayerLoad(IKernel kernel, IGameObject player) {
        IRecord rec = player.getRecord("userBatterySkinList");
        int row = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), 0);
        if (row == -1) {
            rec.addRow(0, -1L, true);
        }
        row = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), m_nBombBattery);// 核弹危机炮台
        if (row == -1) {
            rec.addRow(m_nBombBattery, -1L, false);
        }

        OnCheckBatterySkin(kernel, player);
        kernel.addHeartBeat("HB_CheckBatterySkin", player, 60000, -1);
    }

    void OnPlayerClassCreate(IKernel kernel, String script) {
        //声明一个player属性，表明正在使用的炮台皮肤，在游玩中到期可暂时不消失
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERYINUSE, ValueType.INT, true, true, true);
        //炮台新功能
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_SPEED, ValueType.FLOAT, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_FUNC1, ValueType.BOOL, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_FUNC2, ValueType.BOOL, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_AUTO, ValueType.INT, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_AUTO_COOL, ValueType.INT, false, true, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_BATTERY_STORE, ValueType.INT, false, true, false);

        kernel.listenPropertyChange(PLAYER_PROPERTY_BATTERYINUSE, "Player", this, "OnPlayerProChanged");
        // 声明表，player拥有的炮台皮肤，可以随意切换
        IRecord userBatterySkinList = kernel.declareRecord(script, "userBatterySkinList", UserBatterySkinColEnum.COL_MAX.ordinal(), 20, false, true, true);
        userBatterySkinList.setColType(UserBatterySkinColEnum.ITEM_ID.ordinal(), ValueType.INT);
        userBatterySkinList.setColType(UserBatterySkinColEnum.END_DATE.ordinal(), ValueType.LONG);
        userBatterySkinList.setColType(UserBatterySkinColEnum.IN_USE.ordinal(), ValueType.BOOL);
    }

    public void OnItemClassCreate(IKernel kernel, String script) {
        kernel.declareProperty(script, PLAYER_PROPERTY_TIMELIMIT, ValueType.INT, false, false, false);
        kernel.declareProperty(script, PLAYER_PROPERTY_SKINID, ValueType.INT, false, false, false);
    }

    void OnPlayerProChanged(IKernel kernel, IGameObject player, String proName, Object oldVal) {
        if (PLAYER_PROPERTY_BATTERYINUSE.equals(proName)) {
            int batteryInUse = player.getInt(proName);
            if (batteryInUse != m_nBombBattery) {
                player.setProperty(PLAYER_PROPERTY_BATTERYINUSEBACKUP, batteryInUse);
            } else {
                int old = (int) oldVal;
                if (old != m_nBombBattery) {
                    player.setProperty(PLAYER_PROPERTY_BATTERYINUSEBACKUP, oldVal);
                }
            }
            updateBatteryProp(player, batteryInUse);
        }
    }

    // 从背包使用 ,Command由背包模块或item模块发来
    public void OnUseItemInBag(IKernel kernel, IGameObject item, Object... objects) {
        IGameObject player = (IGameObject) objects[0];
        //String itemId = item.GetString("Id");
        int skinId = item.getInt(PLAYER_PROPERTY_SKINID);
        logger.info(" == enter OnUseItemInBag, player:{}, item:{}   ID: {}", player.getProperty(PLAYER_PROPERTY_NAME), item.getString(PLAYER_PROPERTY_NAME),item.getProperty("Id"));

        IRecord rec = player.getRecord("userBatterySkinList");
        int row = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), skinId);
        if (row == -1) {// 如果是未使用过的炮台皮肤(或已过期被删除过的)
            if (item.getInt(PLAYER_PROPERTY_TIMELIMIT) != -1) {// 如果有时效
                // 增加一行并设置好失效日期
                rec.addRow(skinId, kernel.getServerTime() + item.getInt(PLAYER_PROPERTY_TIMELIMIT) * 3600 * 1000L, false);
            } else {
                // 增加一行
                rec.addRow(skinId, -1L, false);
            }
        } else {// 已使用过并在有效期内的
            long timeEnd = rec.getLong(row, UserBatterySkinColEnum.END_DATE.ordinal());
            if (timeEnd != -1l) {
                if (item.getInt(PLAYER_PROPERTY_TIMELIMIT) != -1) {
                    // 有效时间累加
                    rec.setValue(row, UserBatterySkinColEnum.END_DATE.ordinal(),
                            timeEnd + item.getInt(PLAYER_PROPERTY_TIMELIMIT) * 3600 * 1000L);
                } else {
                    rec.setValue(row, UserBatterySkinColEnum.END_DATE.ordinal(), -1L);
                }
            }
        }
        // 自动佩戴
        int row_to_use = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), skinId);
        if (row_to_use != -1) {
            int row_in_use = rec.findRow(0, UserBatterySkinColEnum.IN_USE.ordinal(), true);
            if (row_in_use != -1) {
                rec.setValue(row_in_use, UserBatterySkinColEnum.IN_USE.ordinal(), false);
            }
            rec.setValue(row_to_use, UserBatterySkinColEnum.IN_USE.ordinal(), true);
            player.setProperty(PLAYER_PROPERTY_BATTERYINUSE, skinId);
        }
    }

    // 从个人信息使用（换装）C2SMsg
    public void OnSelectBatterySkin(IKernel kernel, IGameObject player, int msgid, byte[] msg)
            throws InvalidProtocolBufferException {
        CustomMsg.Int32 selectMsg = CustomMsg.Int32.parseFrom(msg);
        int skinId = selectMsg.getValue();
        IRecord rec = player.getRecord("userBatterySkinList");
        int row_to_use = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), skinId);
        if (row_to_use != -1) {
            int row_in_use = rec.findRow(0, UserBatterySkinColEnum.IN_USE.ordinal(), true);
            if (row_in_use != -1) {
                rec.setValue(row_in_use, UserBatterySkinColEnum.IN_USE.ordinal(), false);
            }
            rec.setValue(row_to_use, UserBatterySkinColEnum.IN_USE.ordinal(), true);
            player.setProperty(PLAYER_PROPERTY_BATTERYINUSE, skinId);
        }
    }

    public void OnCheckBatterySkin(IKernel kernel, IGameObject player) {
        int skinid = player.getInt(PLAYER_PROPERTY_BATTERYINUSE);
        // 检测玩家炮台表项过期情况，过期则删除这个炮台皮肤
        long serverTime = kernel.getServerTime();
        IRecord rec = player.getRecord("userBatterySkinList");
        int rows = rec.getRows();
        for (int i = 0; i < rows; i++) {
            long timeLimit = rec.getLong(i, UserBatterySkinColEnum.END_DATE.ordinal());
            if (timeLimit != -1 && serverTime > timeLimit) {
                if (rec.getInt(i, UserBatterySkinColEnum.ITEM_ID.ordinal()) == skinid) {
                    player.setProperty(PLAYER_PROPERTY_BATTERYINUSE, 0);
                    int pos = rec.findRow(0, UserBatterySkinColEnum.ITEM_ID.ordinal(), 0);
                    if (pos != -1) {
                        rec.setValue(pos, UserBatterySkinColEnum.IN_USE.ordinal(), true);
                    }
                }
                rec.removeRow(i);
                i--;
                rows--;
            }
        }
    }
}
