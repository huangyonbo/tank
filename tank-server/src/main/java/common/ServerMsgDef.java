package common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public enum ServerMsgDef {
    MMSG_ADD_DESK(0, "增加桌子"),
    MMSG_DEL_DESK(1, "删除桌子"),
    MMSG_SIT_DOWN(2, "坐下"),
    MMSG_STAND_UP(3, "站起"),
    MMSG_MATCH(4, "匹配"),
    MMSG_CREATE_DESK(5, "创建桌子"),

    MMSG_ALLOC_ROBOT(6, "分配机器人"),
    MMSG_FREE_ROBOT(7, "释放机器人"),

    PUBMSG_ADD_ROOM(8, "添加房间数据公共数据"),
    PUBMSG_SET_TOTALPW(9, "设置房间总玩总赢"),
    PUBMSG_UPDATE_TOTALPW(10, "设置房间总玩总赢"),
    PUBMSG_CHECK_PROLIST(11, "检测属性榜单"),
    PUBMSG_UPDATE_VIP(12, "榜单更新VIP"),
    PUBMSG_STOP_LIST(13, "停止进榜"),
    PUBMSG_CLEAR_LIST(14, "清榜"),
    PUBMSG_ENTER_ROOM(15, "玩家进入房间"),
    PUBMSG_LEAVE_ROOM(16, "玩家离开房间"),
    PUBMSG_PLAYER_DATA(17, "玩家信息"),
    PUBMSG_UPDATE_LIST_NAME(18, "更新排行榜中的名字"),
    PUBMSG_UPDATE_LIST_VIP(19, "更新排行榜中的VIP"),

    PUBMSG_ADD_JOCKPORT(20, "增加奖池"),
    PUBMSG_ADD_WORD(21, "增加一套"),
    PUBMSG_CHECK_VERSION(22, "检测版本"),

    PUBMSG_ARENA_CHECKNEXT(23, "检测下一状态"),
    PUBMSG_ARENA_CHECKLIST(24, "检测排行榜"),
    PUBMSG_ARENA_ADDONE(25, "竞技场+1人"),
    PUBMSG_ARENA_SUBONE(26, "竞技场-1人"),

    PUBMSG_ADD_BOSS_DESK(27, "增加BOSS房"),
    PUBMSG_ENTER_BOSS_DESK(28, "进入BOSS房"),
    PUBMSG_LEAVE_BOSS_DESK(29, "离开BOSS房"),
    PUBMSG_DEL_BOSS_DESK(30, "BOSS房结束"),

    B2P_SET_ARENA(31, "设置竞技场"),
    B2P_DEL_ARENA(32, "删除竞技场"),
    B2P_OPEN_ARENA(33, "打开竞技场"),
    B2P_CLOSE_ARENA(34, "关闭竞技场"),
    B2P_GET_ARENA(35, "获取竞技场信息"),

    G2P_ARENA_ADDSIGN(36, "竞技场报名"),
    P2G_STOP_ARENA(37, "停止竞技场"),
    P2G_CLOSE_ARENA(38, "关闭竞技场"),
    P2G_TIP_ARENA(39, "第一名提示"),

    M2P_CLOSE_ALL_ARENA(40, "关闭所有竞技场"),
    B2G_REFRESH_ACTIVITY(41, "刷新活动配置"),
    B2G_CLOSE_ACTIVITY(42, "关闭活动"),

    B2G_REFRESH_VERREWARD(43, "刷新活动配置"),
    B2G_CLOSE_VERREWARD(44, "关闭活动"),

    B2G_REFRESH_DAILYREWARD(45, "刷新活动配置"),
    B2G_CLOSE_DAILYREWARD(46, "关闭活动"),

    P2G_REFRESH_LIST(47, "榜单重置，刷新榜单"),

    PUBMSG_MAINTAIN(48, "通知pub后台设置维护计划"),
    MMSG_MAINTAIN(49, "通知Game后台维护计划"),          // 通知Game后台维护计划，（群发邮件，跑马灯，踢人等（支持by渠道））
    B2P_PUBNOTICE(50, "通知pub后台新增公告（登录公告）"),
    B2P_REPNOTICE(51, "通知pub后台删除公告（登录公告）"),
    B2G_GAMENOTICE(52, "通知Game后台要跑马游戏公告"),

    B2G_SYSMAIL(53, "通知Game后台要发送系统邮件"),
    B2G_PAYMAIL(54, "通知Game后台要发送充值邮件"),
    B2G_CHECK_ATTACHMENT(55, "通知Game后台要检查附件合法性"),

    B2G_ROOM_PLAYER_DATA(56, "B2G_ROOM_PLAYER_DATA"),

    B2P_ALLROOM_DATA(57, "后台请求Game房间（厅）信息"),
    P2G_ALLROOM_RUNINFO(58, "回复game房间在线人数，总玩总得信息"),
    G2B_ALLROOM_DATA(59, "game返回给back的厅信息"),

    B2G_GET_ONLINE_PLAYER(60, "获取一个在线玩家的资料"),
    B2G_DEDUCT_ITEMS(61, "通知game扣除用户道具"),
    B2G_FREEZE_PLAYER(62, "通知game冻结用户"),
    B2G_UNFREEZE_PLAYER(63, "通知game解冻用户"),
    B2G_GET_TOTAL_PW(64, "获取总玩总赢"),
    B2G_SET_TOTAL_PW(65, "设置总玩总赢"),

    B2P_RANKING_DATA(66, "获取pub服排行榜数据"),
    B2P_FEEDBACK_SERVICE(67, "玩家反馈客服设置"),

    B2M_UPDATE_ROBOT(68, "更新机器人配置"),
    M2G_ROBOT_OFFLINE(69, "机器人下线"),
    M2G_ROBOT_UPDATE(70, "更新机器人配置到游戏服"),
    G2M_ROBOT_UPDATE(71, "更新机器人配置到匹配服"),
    G2M_REQ_SIT_DOWN(72, "请求坐下"),
    M2G_CREATE_CUSTOM_DESK(73, "创建自定义桌子"),
    G2M_REQ_DESK_LIST(74, "请求至尊选座列表"),
    M2G_BROAD_SEAT(75, "广播至尊选座座位信息"),
    G2M_CREATE_PW(76, "创建密码桌"),
    G2M_ENTER_PW(77, "进入密码桌"),
    G2M_UPDATE_BG(78, "更新背景"),
    M2G_UPDATE_BG(79, "更新背景"),
    B2M_UPDATE_GAME(80, "更新至尊选座桌子"),
    B2M_UPDATE_CFG(81, "更新配置"),
    B2M_CLOSE_GAME(82, "关闭至尊选座桌子"),

    M2G_KICK_PLAYERS(83, "踢出所有玩家"),
    M2G_UPDATE_DESK(84, "更新桌子配置"),
    M2G_REMOVE_CFG(85, "删除桌子配置"),
    M2G_REMOVE_DESK(86, "删除桌子"),
    M2G_UPDATE_PW_CFG(87, "更新密码桌配置"),
    M2G_UPDATE_PARAM(88, "更新参数"),

    PUBMSG_BUY_GOODS(89, "通知商城购买物品"),
    PUBMSG_BUY_STONE(90, "购买灵石"),
    MMSG_CHAT_SET(91, "聊天禁言设置"),

    PUBMSG_ADD_SUPPORT(92, "增加补给礼包购买次数"),
    PUBMSG_START_SUPPORT(93, "开启补给礼包活动"),
    PUBMSG_ADD_MYSTERY(94, "增加神秘礼包购买次数"),
    PUBMSG_START_MYSTERY(95, "开启神秘礼包活动"),
    PUBMSG_ADD_LIMIT(96, "增加神秘礼包购买次数"),
    PUBMSG_START_LIMIT(97, "开启神秘礼包活动"),

    M2G_UPDATE_CFG(98, "更新配置文件"),
    M2G_UPDATE_JAR(99, "更新算法jar包"),
    M2G_RUN_JAR(100, "运行jar包"),

    MASTER_GM_SWITCH(101, "GM命令开关"),
    MASTER_ACTIVE_PUSH(102, "支付结果主动推送"),

    H2G_PAY_BACK(103, "通知"),
    PUBMSG_ON_CHANGE_DAY(104, "日期改变通知"),
    PUBMSG_ON_CHANGE_WEEK(105, "日期改变通知"),
    PUBMSG_INIT_PLAYER_COUNT(106, "初始化虚拟玩家数据"),
    PUBMSG_PLAYER_LOGIN(107, "玩家登录"),
    PUBMSG_PLAYER_LOGOUT(108, "玩家退出游戏"),
    PUBMSG_COPY_CERTAIN_WEEK_LIST(109, "复制特定排行榜数据"),
    PUBMSG_UPDATE_ACTIVITY_RANK_STATUS(110, "更新活动排行榜奖品领取状态"),
    PUBMSG_UPDATE_ACTIVITY_RANK_PROGRESS(111, "更新活动排行进展状态"),
    PUBMSG_ON_HIT_EGG_AWARD_CHANGE(112, "砸金蛋累计奖金变化"),
    PUBMSG_ON_ADD_HIT_EGG_AWARD_SHOW(113, "增加砸金蛋大奖播报"),
    PUBMSG_ON_CLEAR_HIT_EGG_AWARD_VALUE(114, "清空砸金蛋累计奖金变化"),
    PUBMSG_GOLD_EGG_CHECK_VER(115, "监测砸金蛋版本"),
    B2P_CHECK_PROLIST(116, "检测机器人属性榜单"),
    B2P_REMOVE_PROLIST(117, "从属性榜单上删除机器人"),
    B2P_RANK_LIST_DATA(118, "获取榜单所有数据"),
    B2P_UPDATE_EXCHANGE(119, "修改商城兑换数据"),
    PUBMSG_ADD_SUPREME_JACKPOT(120, "增加竞技场排行榜奖池"),
    PUBMSG_CLEAR_SUPREME_JACKPOT(121, "清空竞技场排行榜奖池"),
    PUBMSG_NEW_RECRUIT(122, "新增被招募者"),
    PUBMSG_UPDATE_RECHARGE(123, "更新本周充值额"),
    B2P_RECRUIT_DATA(124, "获取pub服招募数据"),
    PUBMSG_RECRUIT_RECHARGE(125, "获取pub服招募好友及粉丝上周充值"),
    PUBMSG_RECRUIT_INFO(126, "获取pub服招募好友及粉丝本周信息"),
    PUBMSG_ADD_FISH_SCORE_JACKPOT(127, "增加鱼分排行奖金池"),
    PUBMSG_CLEAR_FISH_SCORE(128, "清空鱼分排行奖金池"),
    PUBMSG_FISH_SCORE_CHECK_VER(129, "检测鱼分排行版本"),
    PUBMSG_BUY_GOODS_BY_CARD(130, "通知兑换卡商城购买"),
    B2P_RECRUITER_DATA(131, "获取pub服招募数据"),
    PUBMSG_REFRESH_MONSTER(132, "刷新怪兽"),
    PUBMSG_BOMB_MONSTER(133, "炸怪兽"),
    PUBMSG_UPDATE_DATA(134, "更新获取公共区数据"),
    PUBMSG_ON_MONSTER_AWARD_SHOW(135, "炸怪兽大奖播报"),
    B2G_GET_CUSTOM_DESK_DATA(136, "查询至尊房桌子玩家信息"),
    G2M_GET_CUSTOM_PW_DESK_DATA(137, "查询至尊密码桌玩家信息"),
    PUBMSG_ACTIVITY_LIST_CHECK_VER(138, "检测活动排行版本"),
    PUBMSG_MONOPOLY_AWARD_SHOW(139, "大富翁大奖播报"),
    PUBMSG_MONOPOLY_ADD_JOIN_POPULATION(140, "大富翁参加的玩家数"),
    PUBMSG_MONOPOLY_CHECK_VER(141, "大富翁版本"),
    PUBMSG_ADD_MONOPOLY_JACKPOT(142, "增加大富翁奖金池"),
    PUBMSG_ADD_FISHING_STACK_JACKPOT(143, "增加捕鱼小栈奖金池"),
    PUBMSG_FISHING_STACK_CHECK_VER(144, "捕鱼小栈版本"),
    PUBMSG_LUCKY_CARD_AWARD_SHOW(145, "幸运翻牌大奖播报"),
    PUBMSG_LUCKY_CARD_CHECK_VER(146, "幸运翻牌版本"),
    PUBMSG_BOMB_MONSTER_CHECK_VER(147, "炸怪兽版本"),
    PUBMSG_ADD_MONSTER_TOTAL_RECHARGE(148, "添加怪兽活动充值"),
    PUBMSG_ADD_HIT_FISH_POINTS_JACKPOT(149, "增加捕鱼积分排行榜奖池"),
    PUBMSG_CLEAR_HIT_FISH_POINTS_JACKPOT(150, "清空捕鱼积分排行榜奖池"),
    PUBMSG_ADD_MONOPOLY_TODAY_RECHARGE(151, "增加大富翁今天充值额"),
    PUBMSG_ADD_MONOPOLY_TODAY_COLOR_TICKET(152, "增加大富翁今天获得话费券数量"),
    PUBMSG_ADD_TREASURE_BOWL_TIMES(153, "增加聚宝盆成功失败次数"),
    P2G_CLOSE_BOSS_BATTLE(154, "异常关闭boss大作战"),
    B2G_UPDATE_GUILD_STATUS(155, "修改公会状态"),
    B2G_UPDATE_MEMBER_POSITION(156, "修改成员职位"),
    B2G_UPDATE_GUILD_INFO(157, "修改公会信息"), // 修改公会信息(公会名称和公会公告)
    B2G_UPDATE_SEA_KING_CONFIG(158, "修改海皇神殿配置"),
    G2M_UPDATE_PLAY_WIN(159, "更新桌子总玩总赢"),
    PUBMSG_ADD_LUCK_PLAYER(160, "三太子幸运榜入榜"),
    M2G_LOOK_DATAORRECORD(161, "查看属性"),
    M2G_UPDATE_PROPERTY(162, "更新属性"),
    M2G_UPDATE_RECORD(163, "更新记录"),
    B2G_UPDATE_PAYSET(164, "更新支付方式"),
    B2P_UPDATE_STORE(165, "更新商场"),
    B2G_UPDATE_GUILD_CONFIG(166, "更新公会设置"),
    PUBMSG_MOJIN_ROOM_ACTIVE_RECORD(167, "保存魔晶房日活数据"),
    PUBMSG_ADD_FISH_POND_PUB_DATA(168, "添加养鱼池pubdata"),
    PUBMSG_STORE_FISH_POND_ROBOT_PW(169, "养鱼池所有玩家打机器鱼的总玩总赢"),
    B2G_NOTIFY_UPDATE_POPUP_CONFIG(170, "通知更新弹窗配置"),
    M2G_CREATE_MYSTERY_LEGEND_DESK(171, "创建秘境传说房间类型桌子"),
    B2G_LIST_MYSTERY_LEGEND_ROOM_PLAYER(172, "查询深海秘境房间玩家列表"),
    B2G_UPDATE_MYSTERY_LEGEND_ROOM_CONFIG(173, "后台修改深海秘境房间配置"),
    B2G_DELETE_MYSTERY_LEGEND_ROOM_CONFIG(174, "后台删除深海秘境房间配置"),
    B2G_GET_PLAYER_TOTALWIN(175, "获取玩家总赢"),
    B2G_GET_NEW_PLAYER_LUCKY_CARD_MALL_STOCK(176, "获取新手七日福卡商城商品库存"),
    B2G_CHANGE_LOTTERY_FISH_CONFIG(177, "修改奖券鱼配置"),
    B2G_UPDATE_RANK_LIST_CONFIG(178, "更新排行榜奖励配置"),
    B2G_DELETE_RANK_LIST_CONFIG(179, "删除排行榜奖励配置"),
    G2M_DELETE_MYSTERY_LEGEND_ROOM_CONFIG(180, "删除深海秘境房间配置"),
    B2G_ADS_CONFIG_CHANGE(181, "广告时间变化"),
    B2G_GET_ONLINE_PLAYER_LIST(182, "获取在线玩家信息列表"),
    B2G_GET_CHANNEL_COLOR_TICKET_DROP_RATIO(183, "获取渠道对应的奖券掉落概率"),
    B2G_GET_ALL_ITEM_SCORE_MAX(184, "获取vip等级对应的道具积分基础参数"),
    B2G_GET_BULLET_VALUE_PARAM(185, "查询金币炮等级对应炮值"),
    B2G_GET_N_BULLET_VALUE_PARAM(186, "查询核能炮等级对应炮值"),
    B2M_ADD_CUSTOM_GAME(187, "新增自定义桌"),
    B2M_UPDATE_CUSTOM_GAME(188, "修改自定义桌配置"),
    B2M_CLOSE_CUSTOM_GAME(189, "关闭自定义桌"),
    B2M_OPEN_CUSTOM_GAME(190, "开放自定义桌"),
    H2G_CHANNEL_DATA_CHANGE(191, "渠道参数变化"),
    B2G_REQ_ASSIGN_POSITION_GUILD_MEMBER(192, "任职公会成员（重构）"),
    B2G_REQ_KICK_OUT_GUILD_MEMBER(193, "踢出公会成员（重构）"),
    B2G_REQ_UPDATE_GUILD_CONFIG(194, "更新公会设置（重构）"),
    B2G_REQ_UPDATE_GUILD(195, "更新公会信息（重构）"),
    B2G_REQ_PROPERTY_ITEM(196, "获取属性道具信息"),
    B2G_REQ_DEDUCT_ITEM(197, "后台扣除道具（重构）"),
    G2M_REQ_MYSTERY_LEGEND_DESK_OBJ_ID(198, "请求桌子的对象id"),
    G2P_REQ_RANK_BIG_KUN(199, "请求活动信息"),
    G2P_REQ_RANK_BIG_KUN_EXCHANGE(200, "改变活动信息"),
    PUB_MSG_VIP_LEVEL_GIFT_CHANGE(201, "vip成长礼包修改信息"),
    PUB_MSG_SPIRIT_STONE_FIGHT_SAVE(202, "灵石大作战"),
    B2G_GET_ONLINE_PLAYER_BombCount(203, "B2G_GET_ONLINE_PLAYER_BombCount"),
    PUB_MSG_THREE_SELECT_ONE_STORE_RESULT(204, "PUB_MSG_THREE_SELECT_ONE_STORE_RESULT"),
    B2G_THREE_SELECT_ONE_WEIGHT_CHANGE(205, "B2G_THREE_SELECT_ONE_WEIGHT_CHANGE"),
    PUB_MSG_BMBC_GAME_STORE(206, "PUB_MSG_BMBC_GAME_STORE"),
    PUB_MSG_FQZS_GAME_STORE(207, "PUB_MSG_FQZS_GAME_STORE"),
    B2G_ACTIVITY_UPDATE_CONFIG(208, "B2G_ACTIVITY_UPDATE_CONFIG"),
    B2C_UPDATE_SEND_ITEM_CONFIG(209, "B2C_UPDATE_SEND_ITEM_CONFIG"),
    B2C_UPDATE_GAEM_DEF_CONFIG(210, "B2C_UPDATE_GAEM_DEF_CONFIG"),
    B2G_UPDATE_GATE_RATE_LIMITER_CONFIG(211, "B2G_UPDATE_GATE_RATE_LIMITER_CONFIG"),
    GM2Back_SYNC_ALL_PLAYER_DATA(212, "GM2Back_SYNC_ALL_PLAYER_DATA"),
    B2C_SET_PLAYER_GAME_DEF(213, "B2C_SET_PLAYER_GAME_DEF"),
    PUB_MSG_SLOT_SHZ_STORE(214, "水浒传"),
    PUB_MSG_SLOT_SHZ_BIBEI_STORE(215, "水浒传"),
    PUB_MSG_SLOT_SHZ_MARRY_STORE(216, "水浒传"),

    PUB_MSG_INVITE_STORE(217, "记录邀请"),
    PUB_MSG_USER_BATCH_WITHDRAW_RECORD_STORE(218, "提现记录批量存储"),
    PUB_MSG_USER_BATCH_CONSUME_DAILY_STORE(219, "玩家内日产出批量存储"),
    PUBMSG_FU_LU_SHUANG_QUAN_AWARD_SHOW(220, "福鹿双全大奖记录"),
    PUBMSG_FU_LU_SHUANG_QUAN_CLEAR_AWARD_REC(221, "福鹿双全清空记录"),

    PUB_MSG_SLOT_SGML_STORE(222, "水果玛丽"),

    PUB_MSG_BRNN_GAME_STORE(223, "百人牛牛"),

    B2G_SET_INVITE_VIP(224, "设置邀请VIP"),

    B2C_SET_STATISTICS_RATIO_CONFIG_TUIGUANG(225, "B2C_SET_STATISTICS_RATIO_CONFIG_TUIGUANG"),
    B2C_SET_STATISTICS_RATIO_CONFIG_CAISHEN(226, "B2C_SET_STATISTICS_RATIO_CONFIG_CAISHEN");

    private static final Logger log = LoggerFactory.getLogger(ServerMsgDef.class);

    private final int id;
    private final String desc;

    private static final Map<Integer, ServerMsgDef> MAP = new HashMap<>();

    static {
        for (ServerMsgDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated server message ID {}", def.id);
            } else {
                MAP.put(def.id, def);
            }
        }
    }

    ServerMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static ServerMsgDef getById(int id) {
        return MAP.get(id);
    }
}
