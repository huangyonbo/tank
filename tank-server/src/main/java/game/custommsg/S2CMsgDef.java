package game.custommsg;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum S2CMsgDef implements MsgDef{

    S2C_OUT_FISH(0, "出鱼"),
    S2C_FISH_DEAD(1, "鱼死亡"),
    S2C_CLEAR_FISH(2, "清理鱼"),
    S2C_SYNC_FIRE(3, "同步开炮"),
    S2C_FORMATION(4, "鱼阵"),
    S2C_TASK_FINISHED(5, "任务完成"),
    S2C_LEVEL_AWARD(6, "等级奖励"),
    S2C_SYCN_CHAT(7, "同步聊天"),
    S2C_LOGIN_REWARD(8, "登录奖励"),
    S2C_ON_USE_SKILL(9, "使用技能"),
    S2C_LIVENESS_GRADE_ACHIEVED(10, "活跃度达成"),
    S2C_ACTIVITY_INFO(11, "活动内容"),
    S2C_SYNC_BATTERY_SKILL(12, "同步炮台技能"),
    S2C_SCROLL_MSG(13, "跑马灯"),
    S2C_STORE_ITEMS(14, "商城物品"),
    S2C_GET_AWARD_SUCCESS_XXXXX(15, "领取奖励成功(废弃)"),
    S2C_MESSAGE_TIP(16, "消息提示"),
    S2C_ITEM_TIP(17, "物品提示"),
    S2C_STORE_BUY_GOODS_RESULT(18, "商城购买结果"),
    S2C_RECOVERY_FMT(19, "恢复鱼阵"),
    S2C_PREORDER_RESP(20, "下单结果"),
    S2C_POP_NOTICE_LIST(21, "公告列表"),
    S2C_ON_USE_BOMB(22, "使用炸弹"),
    S2C_FEED_BACK_SERVICE(23, "客服反馈"),
    S2C_UNLOCK_NEWHAND_RESULT_XXXXX(24, "新手解锁(废弃)"),
    S2C_RELIEF_GOLD_RESULT(25, "救济金结果"),
    S2C_VIP_LEVEL_UP(26, "VIP升级"),
    S2C_QUIT_UNOPTION(27, "长时间未操作"),
    S2C_ACHIEVEMENT_TIP(28, "成就提示"),
    S2C_SKILL_FISH_DEAD(29, "技能鱼死亡"),
    S2C_ON_USE_TINDER(30, "使用火种"),
    S2C_HIT_EGG_RESP(31, "砸金蛋结果"),
    S2C_EGG_GOLD_INFO(32, "金蛋信息"),
    S2C_RES_STONE_STOCK(33, "灵石库存"),
    S2C_ARENA_RES(34, "竞技场结果"),
    S2C_BOSS_RES(35, "BOSS结果"),

    S2C_ACTIVITY_LIST(36, "活动列表"),
    S2C_CLOSE_ACTIVITY(37, "关闭活动"),
    S2C_REFRESH_ACTIVITY(38, "刷新活动"),
    S2C_RECV_SENDITEM(39, "赠送道具"),
    S2C_RECRUIT_BIND_RESULT(40, "绑定分享码"),
    S2C_SCROLL_MSG_2(41, "维护消息"),
    S2C_START_TINDER(42, "开始火种"),
    S2C_GM_CMD_RES(43, "GM结果"),
    S2C_BOMB_MONSTER_RESULT(44, "炸怪兽结果"),
    S2C_BOMB_MONSTER_AWARD(45, "炸怪兽广播"),
    S2C_BOMB_MONSTER_RECORD(46, "炸怪兽榜单"),
    S2C_DICING_RESULT(47, "大富翁结果"),
    S2C_FISHING_STACK_LIST_DATA(48, "捕鱼榜单"),
    S2C_LUCKY_CARD_START(49, "翻牌开始"),
    S2C_LUCKY_CARD_INVERT(50, "翻牌操作"),
    S2C_LUCKY_CARD_REFRESH(51, "翻牌刷新"),
    S2C_SPIRIT_SCORE(52, "灵石积分"),
    S2C_NEWER_WELFARE_LOTTERY_RESULT(53, "新手抽奖"),
    S2C_PRE_OUT_THUNDER_DRAGON(54, "雷龙预告"),
    S2C_TRIGGER_ANTIINDULGENCE(55, "防沉迷"),
    S2C_OPEN_AUTHEN_UI(56, "实名认证UI"),
    S2C_BIND_PROXY_OK(57, "绑定代理成功"),
    S2C_FUNC_FISH_UI_CHANGE(58, "功能鱼UI变化"),
    S2C_SKILL_FISH_SKILL_INFO(59, "技能鱼技能"),
    S2C_FUNC_FISH_SKILL_INIT(60, "技能鱼初始化"),
    S2C_FUNC_FISH_SKILL_RESULT(61, "技能鱼结果"),
    S2C_SNYC_FISH_POSITIONS(62, "同步鱼位置"),
    S2C_SKILL_FROEN_OVER_MSG(63, "冰冻结束"),
    S2C_GUILD_MSG(64, "公会消息"),
    S2C_FISH_POND_INFO(65, "养鱼池信息"),
    S2C_FISH_POND_FISH_DIE(66, "鱼池鱼死亡"),
    S2C_FISH_POND_MSG_RECORD(67, "鱼池记录"),
    S2C_FISH_POND_FISH_DIE_ALL(68, "鱼池全死"),
    S2C_POPUP_CONFIG(69, "弹窗配置"),
    S2C_REAL_NAME_AUTH(70, "实名认证结果"),
    S2C_ENTER_MYSTERY_LEGEND(71, "进入秘境"),
    S2C_KICK_OUT_MYSTERY_LEGEND(72, "踢出秘境"),
    S2C_NOTIFY_MYSTERY_DRAGON_BOX_RESULT(73, "神龙宝箱"),
    S2C_JACKPOT_FISH_DEAD_FUNC(74, "巨鲲结果"),
    S2C_FISH_LEAVE(75, "鱼离场"),
    S2C_NEW_YEAR_SEVEN_PKG_RESULTS(76, "新年礼包"),
    S2C_TIME_LIMIT_BURNOUT_AWARD(77, "限时奖励"),
    S2C_TIME_LIMIT_BURNOUT_PHYSICAL_AWARD(78, "实物奖励"),
    S2C_Three_Select_State(79, "三选状态"),
    S2C_THREE_SELECT_GAME_INFO(80, "三选信息"),
    S2C_THREE_SELECT_ONE_REWARD(81, "三选奖励"),

    S2C_BMBC_LEADER_CHANGE(82, "BMBC上庄变化"),
    S2C_BMBC_GAME_CHANGE(83, "BMBC游戏变化"),
    S2C_BMBC_GAME_WIN_RESULT(84, "BMBC结果"),
    S2C_BMBC_GAME_LEADER_RESULT(85, "BMBC庄结果"),
    S2C_BMBC_GAME_DOWN_LEADER(86, "BMBC下庄"),

    S2C_FQZS_GAME_STATE_CHANGE(87, "飞禽状态"),
    S2C_FQZS_LEADER_CHANGE(88, "飞禽上庄"),
    S2C_FQZS_GAME_DETAIL(89, "飞禽下注"),
    S2C_FQZS_GAME_RESULT(90, "飞禽结果"),
    S2C_FQZS_LEADER_GAME_RESULT(91, "飞禽庄结果"),
    S2C_FQZS_GET_GAME_RESULT(92, "飞禽获取结果"),
    S2C_ACTIVITY_REFRESH_CONFIG(93, "活动刷新"),
    S2C_FQZS_GAME_ADD_VALUE_BROADCAST(94, "飞禽下注广播"),
    S2C_FQZS_GAME_DOWN_LEADER(95, "飞禽下庄"),

    S2C_FUNC_FISH_UI_CHANGE_NEW(96, "功能鱼UI新"),
    S2C_SMALL_GAME_TIP(97, "小游戏提示"),
    S2C_BMBC_GAME_DETAIL(98, "BMBC下注"),
    S2C_PLAYER_PLACE(99, "玩家位置"),
    S2C_SYNC_FISH278_PRIZE_POOL(100, "奖池"),

    S2C_BRNN_LEADER_CHANGE(101, "牛牛上庄"),
    S2C_BRNN_GAME_CHANGE(102, "牛牛变化"),
    S2C_BRNN_GAME_WIN_RESULT(103, "牛牛结果"),
    S2C_BRNN_GAME_LEADER_RESULT(104, "牛牛庄结果"),
    S2C_BRNN_GAME_DOWN_LEADER(105, "牛牛下庄"),
    S2C_BRNN_GAME_DETAIL(106, "牛牛下注"),

    S2C_PLAYER_GET_INVITE_VIP(107, "邀请VIP"),

    S2C_END(9999, "结束");

    private final int id;
    private final String desc;

    private static final java.util.Map<Integer, S2CMsgDef> MAP = new java.util.HashMap<>();

    static {
        for (S2CMsgDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated message ID  {}", def.id);
            }else {
                MAP.put(def.id, def);
            }
        }
    }

    S2CMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static S2CMsgDef getById(int id) {
        return MAP.get(id);
    }
}