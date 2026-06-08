package game.modules.weapon;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import framework.game.*;
import framework.mybatis.domain.PlayerWeaponState;
import framework.mybatis.service.impl.PlayerWeaponStateService;
import game.custommsg.CustomMsg;
import game.custommsg.RequestMsgDef;
import game.custommsg.ServerCodeDef;
import game.modules.activities.code.JSONResult;
import game.modules.activities.code.ResponseCode;
import game.modules.utils.UtilFunc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 武器模块
 */
public class WeaponModule implements ILogicModule {
    /**
     * 与 WeaponsUpgradesCost.xml 中 WeaponTypes 0~14 一致
     */
    private static final int WEAPON_TYPE_COUNT = 15;
    /**
     * lv1~lv6 共 6 档升级，最高等级为 6
     */
    private static final int WEAPON_MAX_LEVEL = 6;


    private Map<Integer, WeaponUpgradesData> m_mapWeapon = new HashMap<>();
    private SpeedUpgradeData speedData;
    private ArmorUpgradeData armorData;

    /**
     * @param kernel
     * @return
     */
    @Override
    public boolean onInit(IKernel kernel) {
        kernel.regRequestMessage(RequestMsgDef.REQ_WEAPON_UPGRADE.getId(), this, "OnReqWeaponUpgrade");
        kernel.regRequestMessage(RequestMsgDef.REQ_SPEED_UPGRADE.getId(), this, "OnReqSpeedUpgrade");
        kernel.regRequestMessage(RequestMsgDef.REQ_ARMOR_UPGRADE.getId(), this, "OnReqArmorUpgrade");
        kernel.regRequestMessage(RequestMsgDef.REQ_WEAPON_GET_DATA.getId(), this, "OnReqWeaponGetData");
        kernel.regEvent(KernelEvent.KEVENT_ON_LINE, "Player", this, "OnPlayerOnLine");

        kernel.regEvent(KernelEvent.KEVENT_ON_UPDATE_CFG, "World", this, "RefreshCfg");
        RefreshCfg(kernel, "res/PlayerBalance/PBArmorUpgradeCost.xml");
        RefreshCfg(kernel, "res/PlayerBalance/PBSpeedUpgradeCost.xml");
        RefreshCfg(kernel, "res/PlayerBalance/PBWeaponsUpgradesCost.xml");
        return true;
    }

    /**
     * 每个玩家默认拥有武器0=1级（仅在未初始化时写入）
     */
    public void OnPlayerOnLine(IKernel kernel, IGameObject player) {
        // 登录时从武器表加载到 player 身上；无数据时补默认值并落库
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            return;
        }
        ensureDefaultWeaponLevel(player);
        List<Object> params = Collections.singletonList(uid);
        kernel.executeSomeToStore(PlayerWeaponStateService.class, "queryByUid", params, (res) -> {
            if (res == null || res.trim().isEmpty()) {
                ensureDefaultWeaponLevel(player);
                persistWeaponState(kernel, player);
                return;
            }
            PlayerWeaponState state = JSONObject.parseObject(res, PlayerWeaponState.class);
            if (state == null) {
                ensureDefaultWeaponLevel(player);
                persistWeaponState(kernel, player);
                return;
            }

            // 回填数据库中的武器相关状态
            if (state.getWeaponsLevels() != null) {
                player.setProperty(PLAYER_WEAPONS_LEVELS, state.getWeaponsLevels());
            }
            if (state.getTankSpeedLevel() != null) {
                player.setProperty(PLAYER_TANK_SPEED_LEVEL, state.getTankSpeedLevel());
            }
            if (state.getTankArmorLevel() != null) {
                player.setProperty(PLAYER_TANK_ARMOR_LEVEL, state.getTankArmorLevel());
            }

            ensureDefaultWeaponLevel(player);
        });
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
    }

    void RefreshCfg(IKernel kernel, String path) {
        if (path.equals("res/PlayerBalance/PBArmorUpgradeCost.xml")) {
            this.armorData = null;
            LoadArmorConfig(kernel, path);
        }
        if (path.equals("res/PlayerBalance/PBSpeedUpgradeCost.xml")) {
            this.speedData = null;
            LoadSpeedConfig(kernel, path);
        }
        if (path.equals("res/PlayerBalance/PBWeaponsUpgradesCost.xml")) {
            m_mapWeapon.clear();
            LoadWeaponConfig(kernel, path);
        }
    }

    void OnReqWeaponUpgrade(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg)
            throws InvalidProtocolBufferException {
        ensureDefaultWeaponLevel(player);
        CustomMsg.String msgData = CustomMsg.String.parseFrom(msg);
        JSONObject jsonObject = JSONObject.parseObject(msgData.getValue());
        int weaponType = jsonObject.getIntValue("weaponType");
        if (weaponType < 0) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_PARAM_ERR);
            return;
        }

        WeaponUpgradesData cfg = m_mapWeapon.get(weaponType);
        if (cfg == null || cfg.price == null) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_WRONG_TYPE);
            return;
        }

        Map<Integer, Integer> levels = parseWeaponLevels(player);
        int cur = levels.getOrDefault(weaponType, 0);
        if (cur >= WEAPON_MAX_LEVEL) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_LEVEL_MAX);
            return;
        }

        // cfg.price: lv1~lv6，对应当前等级 cur(0~5)
        if (cur < 0 || cur >= cfg.price.length) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_LEVEL_MAX);
            return;
        }

        int cost = cfg.price[cur];
        long gold = player.getLong(PLAYER_MONEY);
        if (gold < cost) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_NEED_PRO);
            return;
        }

        player.setProperty(PLAYER_MONEY, gold - cost, IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                "weapon upgrade type=" + weaponType);
        levels.put(weaponType, cur + 1);
        player.setProperty(PLAYER_WEAPONS_LEVELS, joinWeaponLevels(levels));
        persistWeaponState(kernel, player);
        UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_SUCCESS);
    }

    void OnReqSpeedUpgrade(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        if (speedData == null || speedData.costs == null || speedData.costs.length == 0) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
            return;
        }
        int maxLevel = speedData.costs.length;
        int cur = player.getInt(PLAYER_TANK_SPEED_LEVEL);
        if (cur >= maxLevel) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_LEVEL_MAX);
            return;
        }
        int cost = speedData.costs[cur];
        long gold = player.getLong(PLAYER_MONEY);
        if (gold < cost) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_NEED_PRO);
            return;
        }
        player.setProperty(PLAYER_MONEY, gold - cost, IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                "tank speed upgrade");
        player.setProperty(PLAYER_TANK_SPEED_LEVEL, cur + 1);
        persistWeaponState(kernel, player);
        UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_SUCCESS);
    }

    void OnReqArmorUpgrade(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        if (armorData == null || armorData.costs == null || armorData.costs.length == 0) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_FAILED);
            return;
        }
        int maxLevel = armorData.costs.length;
        int cur = player.getInt(PLAYER_TANK_ARMOR_LEVEL);
        if (cur >= maxLevel) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_LEVEL_MAX);
            return;
        }
        int cost = armorData.costs[cur];
        long gold = player.getLong(PLAYER_MONEY);
        if (gold < cost) {
            UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_NEED_PRO);
            return;
        }
        player.setProperty(PLAYER_MONEY, gold - cost, IKernel.PlayerLogType.PROP_CHANGE.ordinal(),
                "tank armor upgrade");
        player.setProperty(PLAYER_TANK_ARMOR_LEVEL, cur + 1);
        persistWeaponState(kernel, player);
        UtilFunc.responseSerCodeStr(kernel, player, reqid, ServerCodeDef.CODE_SUCCESS);
    }

    /**
     * 返回前端：武器等级、坦克速度等级、坦克护甲等级
     */
    void OnReqWeaponGetData(IKernel kernel, IGameObject player, int msgid, int reqid, byte[] msg) {
        ensureDefaultWeaponLevel(player);
        JSONResult result = ResponseCode.Success.toJSONResult();
        player.setProperty(PLAYER_MONEY, 10000000);
        // 新格式会返回 map；旧格式能被 parseWeaponLevels 兼容解析
        Map<String, Integer> weaponLevels = parseWeaponLevels(player).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));
        result.put("weaponLevels", weaponLevels);
        result.put("tankSpeedLevel", player.getInt(PLAYER_TANK_SPEED_LEVEL));
        result.put("tankArmorLevel", player.getInt(PLAYER_TANK_ARMOR_LEVEL));

        kernel.response(player, reqid, result);
    }

    /**
     * `PLAYER_WEAPONS_LEVELS` 新格式：`type=level;type=level;...`
     * 兼容旧格式：`level0|level1|...`（把数组下标当作当时的 weaponType）
     */
    private static Map<Integer, Integer> parseWeaponLevels(IGameObject player) {
        Map<Integer, Integer> map = new HashMap<>();
        String s = player.getString(PLAYER_WEAPONS_LEVELS);
        if (s == null) {
            return map;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return map;
        }

        // 新格式：包含 '='
        if (s.indexOf('=') >= 0) {
            String[] pairs = s.split(";");
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.isEmpty()) {
                    continue;
                }
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                try {
                    int type = Integer.parseInt(kv[0].trim());
                    int level = Integer.parseInt(kv[1].trim());
                    map.put(type, level);
                } catch (NumberFormatException ignored) {
                    // 忽略脏数据
                }
            }
            return map;
        }

        // 旧格式：用 '|' 分隔
        if (s.indexOf('|') >= 0) {
            String[] parts = s.split("\\|");
            for (int i = 0; i < Math.min(parts.length, WEAPON_TYPE_COUNT); i++) {
                try {
                    map.put(i, Integer.parseInt(parts[i].trim()));
                } catch (NumberFormatException ignored) {
                    map.put(i, 0);
                }
            }
        }

        return map;
    }

    private static String joinWeaponLevels(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        java.util.ArrayList<Integer> keys = new java.util.ArrayList<>(map.keySet());
        keys.sort(Integer::compareTo);
        StringBuilder sb = new StringBuilder(keys.size() * 4);

        boolean first = true;
        for (Integer type : keys) {
            int level = map.getOrDefault(type, 0);
            if (level <= 0) {
                continue;
            }
            if (!first) {
                sb.append(';');
            }
            first = false;
            sb.append(type).append('=').append(level);
        }
        return sb.toString();
    }

    private static void ensureDefaultWeaponLevel(IGameObject player) {
        Map<Integer, Integer> levels = parseWeaponLevels(player);
        if (levels.getOrDefault(0, 0) <= 0) {
            levels.put(0, 1);
            player.setProperty(PLAYER_WEAPONS_LEVELS, joinWeaponLevels(levels));
        }
    }

    private static void persistWeaponState(IKernel kernel, IGameObject player) {
        int uid = player.getInt(PLAYER_PROPERTY_UID);
        if (uid <= 0) {
            return;
        }
        PlayerWeaponState state = new PlayerWeaponState();
        state.setUid(uid);
        state.setWeaponsLevels(player.getString(PLAYER_WEAPONS_LEVELS));
        state.setTankSpeedLevel(player.getInt(PLAYER_TANK_SPEED_LEVEL));
        state.setTankArmorLevel(player.getInt(PLAYER_TANK_ARMOR_LEVEL));
        kernel.executeSomeToStore(PlayerWeaponStateService.class, "upsertByUid", Collections.singletonList(state), null);
    }

    boolean LoadWeaponConfig(IKernel kernel, String path) {
        ICfgReader cfg = kernel.loadXmlConfig(path);
        if (cfg == null) {
            return false;
        }
        int count = cfg.getItemCount();
        for (int i = 0; i < count; ++i) {
            int weaponTypes = cfg.getInt(i, "WeaponTypes");
            int[] price = new int[6];
            for (int j = 1; j <= 6; j++) {
                int target = cfg.getInt(i, "lv" + j);
                price[j - 1] = target;
            }
            WeaponUpgradesData weaponData = new WeaponUpgradesData();
            weaponData.weaponTypes = weaponTypes;
            weaponData.price = price;
            m_mapWeapon.put(weaponTypes, weaponData);
        }
        return true;
    }

    boolean LoadSpeedConfig(IKernel kernel, String path) {
        ICfgReader cfg = kernel.loadXmlConfig(path);
        if (cfg == null) {
            return false;
        }
        int count = cfg.getItemCount();
        int[] costs = new int[count];
        for (int i = 0; i < count; ++i) {
            costs[i] = cfg.getInt(i, "speedUpgradeCost");
        }
        SpeedUpgradeData data = new SpeedUpgradeData();
        data.costs = costs;
        this.speedData = data;
        return true;
    }

    boolean LoadArmorConfig(IKernel kernel, String path) {
        ICfgReader cfg = kernel.loadXmlConfig(path);
        if (cfg == null) {
            return false;
        }
        int count = cfg.getItemCount();
        int[] costs = new int[count];
        for (int i = 0; i < count; ++i) {
            costs[i] = cfg.getInt(i, "armorUpgradeCost");
        }
        ArmorUpgradeData data = new ArmorUpgradeData();
        data.costs = costs;
        this.armorData = data;
        return true;
    }

    static class WeaponUpgradesData {
        int weaponTypes;
        int[] price;
    }

    static class SpeedUpgradeData {
        int[] costs;
    }

    static class ArmorUpgradeData {
        int[] costs;
    }
}
