package game.custommsg;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

// Wire IDs are explicit. Never use ordinal(): retired definitions may be removed.
@Slf4j
public enum RequestMsgDef implements MsgDef{
    REQ_UNLOCK_BV(0, "解锁BV"),
    REQ_HEADID(4, "根据uid请求头像id"),
    REQ_HEADURL(5, "根据头像id请求头像url"),
    REQ_ROOM_PLAYERS(8, "请求房间人数"),
    REQ_CHANGE_NAME(10, "请求改名"),
    REQ_PLAYER_DATA(11, "请求玩家信息"),
    REQ_COMBINE(13, "请求合成"),
    REQ_BUY_FUNCITEM(14, "请求购买功能道具并立刻使用"),
    REQ_UNLOCK_TO(18, "解锁到"),
    REQ_STONE_EXCHANGE(22, "灵石兑换"),
    REQ_REDEEMCODE(23, "兑换码兑换道具"),
    REQ_ORDER_RES(29, "请求订单结果"),
    REQ_BIND_CHANNEL(31, "绑定渠道"),
    REQ_BOMB_TO_AMMO(42, "弹头转弹药"),
    REQ_AMMO_TO_BOMB(43, "弹药转弹头"),
    REQ_SMITHING(44, "炮台锻造"),
    REQ_BOMB_EDEN_OPEN_CONFIG(46, "炸弹乐园配置"),
    REQ_COMBINE_TO_GOLD(48, "合成换金币"),
    REQ_TRANSFORM_TO_STONE_TIMES(53, "兑换次数"),
    REQ_CHANGE_PASSWORD(54, "修改密码"),
    REQ_BIND_PHONE(55, "绑定手机号"),
    REQ_UN_BIND_PHONE(56, "解绑手机号"),
    REQ_SET_TEMP_CELLPHONE(65, "输入手机号"),
    REQ_ROOM_FISHLIST(66, "房间鱼列表"),
    REQ_TOURIST_BIND_PHONE(69, "游客绑定手机"),
    REQ_GET_BULLETVAL_LIST(70, "炮值列表"),
    REQ_GET_PAY_WAYS(71, "支付方式"),
    REQ_ITEMRECYCLE(74, "道具回收"),
    REQ_ACTIVITY_REWARD(77, "活动奖励"),
    REQ_REAL_NAME(87, "实名认证"),
    REQ_GOLD_ROOM_CONFIG(91, "金币场配置"),
    EQ_LDK_DO_PLAY(94, "龙王遗赠"),
    REQ_ACTIVITY_INFO(100, "活动信息"),
    REQ_CHANGE_PASSWORD_NEW(101, "新修改密码"),
    REQ_SEND_ITEM_NEW(105, "赠送道具新"),
    REQ_SEND_HISTORY(106, "赠送历史"),
    REQ_CORNUCOPIA_VALUE(108, "聚宝盆值"),
    REQ_CORNUCOPIA_HISTORY(109, "聚宝盆历史"),
    REQ_ACTIVITY_CONFIG_NEW(110, "新活动配置"),

    REQ_SPEED_UPGRADE(10002, "速度升级"),
    REQ_ARMOR_UPGRADE(10003, "护甲升级"),
    REQ_WEAPON_UPGRADE(10004, "武器升级"),
    REQ_LEVEL_FINISH(10005, "关卡通关"),
    REQ_LEVEL_START(10006, "关卡开始"),
    REQ_LEVEL_RANK_LIST(10007, "关卡排行"),
    REQ_ACHIEVEMENT_GET_STATUS(10008, "获取成就领取状态"),
    REQ_CUSTOM_MAP_SAVE(10009, "保存自定义地图"),
    REQ_CUSTOM_MAP_LIST(10010, "获取我的地图列表"),
    REQ_CUSTOM_MAP_DELETE(10011, "删除我的地图"),
    REQ_CUSTOM_MAP_PUBLIC_LIST(10012, "获取公共地图列表"),
    REQ_CUSTOM_MAP_GET_BY_ID(10013, "按地图ID获取地图"),
    REQ_CUSTOM_MAP_LIKE_BY_ID(10014, "自定义地图点赞"),
    REQ_CUSTOM_MAP_START(10015, "自定义地图开始"),
    REQ_CUSTOM_MAP_END(10016, "自定义地图结束"),

    REQ_GOOGLE_PLAY_BUY_GOLD(10018, "Google Play buy gold"),
    REQ_GOOGLE_PLAY_PREPARE(10019, "Google Play purchase eligibility and account id"),
    REQ_GOOGLE_PLAY_PRODUCTS(10020, "Google Play product list"),
    REQ_REWARDED_AD_VERIFY(10021, "Rewarded ad verify and token issue"),
    REQ_REWARDED_AD_CLAIM(10022, "Rewarded ad claim gold by token");

    private final int id;
    private final String desc;

    private static final Map<Integer, RequestMsgDef> MAP = new HashMap<>();

    static {
        for (RequestMsgDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated message ID  {}", def.id);
            }else {
                MAP.put(def.id, def);
            }
        }
    }

    public static RequestMsgDef getById(int id) {
        return MAP.get(id);
    }

    RequestMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }
}
