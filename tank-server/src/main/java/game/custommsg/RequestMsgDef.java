package game.custommsg;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

//客户端请求消息定义
@Slf4j
public enum RequestMsgDef implements MsgDef{
    REQ_UNLOCK_BV(0, "解锁BV"),
    REQ_CHECK_SEND_INFO(1, "检测赠送玩家信息"),
    REQ_SEND_ITEM(2, "赠送道具"),
    REQ_PRO_LIST(3, "获取属性排行榜"),
    REQ_HEADID(4, "根据uid请求头像id"),
    REQ_HEADURL(5, "根据头像id请求头像url"),
    REQ_SERVER_TIME(6, "请求服务器时间"),
    REQ_COLOR_GOLD(7, "彩金抽奖"),
    REQ_ROOM_PLAYERS(8, "请求房间人数"),
    REQ_ENTER_ROOM(9, "请求进入房间"),
    REQ_CHANGE_NAME(10, "请求改名"),
    REQ_PLAYER_DATA(11, "请求玩家信息"),
    REQ_UNLOCK_NEWHAND(12, "新手任务一键解锁"),
    REQ_COMBINE(13, "请求合成"),
    REQ_BUY_FUNCITEM(14, "请求购买功能道具并立刻使用"),
    REQ_ACTIVITY_RANK(15, "获取活动排行榜"),
    REQ_ACTIVITY_RANK_REWARD(16, "活动排行榜领奖"),
    REQ_DAILY_DRAW(17, "请求每日抽奖奖励"),
    REQ_UNLOCK_TO(18, "解锁到"),
    REQ_LUCKY_NUMBER_LIST(19, "获取砸金蛋幸运数字排行榜"),
    REQ_NATIONAL_REWARD(20, "国庆抽奖"),
    REQ_NATIONAL_INFO(21, "请求国庆活动信息"),
    REQ_STONE_EXCHANGE(22, "灵石兑换"),
    REQ_REDEEMCODE(23, "兑换码兑换道具"),
    REQ_ARENA_LIST(24, "请求竞技场比赛列表"),
    REQ_ENTER_ARENA(25, "请求参加比赛"),
    REQ_BOSS_LIST(26, "BOSS房间列表"),
    REQ_CREATE_BOSS(27, "创建BOSS战"),
    REQ_ENTER_BOSS(28, "进入BOSS战"),
    REQ_ORDER_RES(29, "请求订单结果"),
    REQ_ARENA_RANK(30, "请求比赛名次"),
    REQ_BIND_CHANNEL(31, "绑定渠道"),
    REQ_NYW_DRAW(32, "新年集字抽奖"),
    REQ_DESK_LIST(33, "请求至尊选座桌子列表"),
    REQ_SIT_DOWN(34, "点击座位坐下"),
    REQ_CREATE_PW(35, "创建密码桌"),
    REQ_ENTER_PW(36, "进入密码桌"),
    REQ_QUICK_ENTER(37, "快速开始"),
    REQ_SUPREME_SINGLE_JACKPOT(38, "至尊单次奖池"),
    REQ_SUPREME_TOTAL_JACKPOT(39, "至尊累计奖池"),
    REQ_BOSS_CFG(40, "获取BOSS配置"),
    REQ_RECRUIT_INFO(41, "好友粉丝招募信息"),
    REQ_BOMB_TO_AMMO(42, "弹头转弹药"),
    REQ_AMMO_TO_BOMB(43, "弹药转弹头"),
    REQ_SMITHING(44, "炮台锻造"),
    REQ_FISH_SCORE_JACKPOT(45, "鱼分奖池"),
    REQ_BOMB_EDEN_OPEN_CONFIG(46, "炸弹乐园配置"),
    REQ_MONSTER_INFO(47, "炸怪兽信息"),
    REQ_COMBINE_TO_GOLD(48, "合成换金币"),
    REQ_MONSTER_RECORD(49, "炸怪兽排行榜"),
    REQ_MONOPOLY_RECORD(50, "大富翁排行榜"),
    REQ_MONOPOLY_JACKPOT(51, "大富翁奖池"),
    REQ_FISHING_STACK_JACKPOT(52, "捕鱼小栈奖池"),
    REQ_TRANSFORM_TO_STONE_TIMES(53, "兑换次数"),
    REQ_CHANGE_PASSWORD(54, "修改密码"),
    REQ_BIND_PHONE(55, "绑定手机号"),
    REQ_UN_BIND_PHONE(56, "解绑手机号"),
    REQ_LUCKY_CARD_LUCKY_RECORD(57, "翻牌排行榜"),
    REQ_HIT_FISH_POINTS_JACKPOT(58, "捕鱼积分奖池"),
    REQ_COMPOUND_BOX(59, "合成宝箱"),
    REQ_OPEN_BOX(60, "开启宝箱"),
    REQ_OPEN_BAG(61, "开启福袋"),
    REQ_UP_ONE_KEY(62, "一键升级"),
    REQ_TREASURE_BOWL_REFRESH(63, "刷新聚宝盆"),
    REQ_TREASURE(64, "聚宝"),
    REQ_SET_TEMP_CELLPHONE(65, "输入手机号"),
    REQ_ROOM_FISHLIST(66, "房间鱼列表"),
    REQ_GUILD_OPERATE(67, "公会操作"),
    REQ_SEA_KINK_CONFIG(68, "海神殿配置"),
    REQ_TOURIST_BIND_PHONE(69, "游客绑定手机"),
    REQ_GET_BULLETVAL_LIST(70, "炮值列表"),
    REQ_GET_PAY_WAYS(71, "支付方式"),
    REQ_CHANGE_DESK(72, "换桌"),
    REQ_TREASURE_IN_SEA_DRAW(73, "海底寻宝抽奖"),
    REQ_ITEMRECYCLE(74, "道具回收"),
    REQ_GUILD_REPOSITORY_OPERATE(75, "仓库操作"),
    REQ_ACTIVITY_CONFIG(76, "活动配置"),
    REQ_ACTIVITY_REWARD(77, "活动奖励"),
    REQ_FISH_POND_PAGE_STATUS(78, "养鱼池界面"),
    REQ_FISH_POND_FISH_INFO(79, "养鱼池鱼信息"),
    REQ_PUSH_FISH(80, "投入鱼"),
    REQ_CATCH_FISH(81, "抓鱼"),
    REQ_REFRESH_FISH_POND(82, "刷新养鱼池"),
    REQ_IS_PERSONAL_DESK_FULL(83, "单人桌是否满"),
    REQ_NAVIGATION_ACTIVITY_CONFIG(84, "航海指南配置"),
    REQ_MERMAID_TREASURE_RECORD(85, "美人鱼记录"),
    REQ_DEBRIS_PKG_REWARD(86, "碎片奖励"),
    REQ_REAL_NAME(87, "实名认证"),
    REQ_DRAW_NEW_PLAYER_LUCKY_CARD(88, "新手福卡"),
    REQ_LUCKY_CARD_MALL_EXCHANGE(89, "福卡兑换"),
    REQ_RANK_LIST_REWARD(90, "排行榜奖励"),
    REQ_GOLD_ROOM_CONFIG(91, "金币场配置"),
    REQ_BIG_KUN_ACTIVITY_INFO(92, "巨鲲活动"),
    REQ_TPR_DO_PLAY(93, "龟相来贺"),
    EQ_LDK_DO_PLAY(94, "龙王遗赠"),
    REQ_FISHING_TASK_REWARD(95, "鱼场任务奖励"),
    REQ_VIP_LEVEL_GIFT(96, "VIP礼包"),
    REQ_SPIRIT_STONE_FIGHT(97, "灵石大作战"),
    REQ_VIP_LEVEL_GIFT_GET_GOLD(98, "VIP领金币"),
    REQ_SPIRIT_STONE_FIGHT_GET_GOLD(99, "灵石领金币"),
    REQ_ACTIVITY_INFO(100, "活动信息"),
    REQ_CHANGE_PASSWORD_NEW(101, "新修改密码"),
    REQ_BOMB_DAILY_RANK(102, "每日排行榜"),
    REQ_BOMB_WEEK_RANK(103, "周排行榜"),
    REQ_CHECK_SEND_PLAYER(104, "查询赠送玩家"),
    REQ_SEND_ITEM_NEW(105, "赠送道具新"),
    REQ_SEND_HISTORY(106, "赠送历史"),
    REQ_BOMB_DAILY_LIMIT_RANK(107, "每日限制榜"),
    REQ_CORNUCOPIA_VALUE(108, "聚宝盆值"),
    REQ_CORNUCOPIA_HISTORY(109, "聚宝盆历史"),
    REQ_ACTIVITY_CONFIG_NEW(110, "新活动配置"),
    REQ_THREE_SELECT_HISTORY(111, "三选历史"),
    REQ_THREE_SELECT_VALUE(112, "三选值"),
    REQ_THREE_SELECT_GAME_DETAIL(113, "三选详情"),
    REQ_THREE_SELECT_GAME_HISTORY(114, "三选记录"),
    REQ_BMBC_ADD_LEADER_QUEUE(115, "上庄"),
    REQ_BMBC_ADD_VALUE(116, "下注"),
    REQ_BMBC_HISTORY_GAME(117, "历史游戏"),
    REQ_BMBC_GET_GAME_DETAIL(118, "获取游戏"),
    REQ_BMBC_DEL_LEADER_QUEUE(119, "删除上庄"),
    REQ_BMBC_HEART_AND_META(120, "心跳"),
    REQ_FQZS_ADD_LEADER_QUEUE(121, "飞禽上庄"),
    REQ_FQZS_DEL_LEADER_QUEUE(122, "飞禽删除"),
    REQ_FQZS_EXIT_GAME(123, "飞禽退出"),
    REQ_FQZS_ADD_VALUE(124, "飞禽下注"),
    REQ_BMBC_EXIT_GAME(125, "退出游戏"),
    REQ_FQZS_GET_GAME_DETAIL(126, "飞禽详情"),
    REQ_BMBC_CANCEL_LEADER_QUEUE(127, "取消上庄"),
    REQ_FQZS_CANCEL_LEADER_QUEUE(128, "飞禽取消"),
    REQ_PLAYER_PLACE(129, "玩家位置"),
    REQ_SHZ_BET(130, "水浒下注"),
    REQ_SHZ_BONUS_BET(131, "水浒奖励"),
    REQ_VIP_GET_INFO(132, "VIP信息"),
    REQ_VIP_BIND_CODE(133, "VIP绑定"),
    REQ_VIP_RECEIVE_AWARD(134, "VIP奖励"),
    REQ_GUN_GET_DATA(135, "炮台数据"),
    REQ_GUN_BUY(136, "购买炮台"),
    REQ_GUN_EQUIP(137, "装备炮台"),
    REQ_FUNC_FISH_278_GET_DATA(138, "福鹿双全"),
    REQ_SGML_BET(139, "水果玛丽下注"),
    REQ_SGML_GET_INFO(140, "水果玛丽信息"),
    REQ_BRNN_ADD_LEADER_QUEUE(141, "牛牛上庄"),
    REQ_BRNN_ADD_VALUE(142, "牛牛下注"),
    REQ_BRNN_GET_GAME_DETAIL(143, "牛牛数据"),
    REQ_BRNN_DEL_LEADER_QUEUE(144, "牛牛删除"),
    REQ_BRNN_CANCEL_LEADER_QUEUE(145, "牛牛取消"),
    REQ_BRNN_EXIT_GAME(145, "牛牛退出"),

    REQ_WEAPON_GET_DATA(10001, "武器数据"),
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

    REQ_AD_GIFT_BAG_CLAIM(10017, "看广告领金币礼包"),



    REQ_END(9999, "结束");

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
