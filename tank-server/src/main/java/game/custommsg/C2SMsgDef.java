package game.custommsg;
//客户端请求消息定义(无返回值)

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum C2SMsgDef implements MsgDef{

    C2S_TEST(0, "测试"),
    C2S_ENTER_ROOM(1, "进入游戏"),
    C2S_LEAVE_ROOM(2, "退出游戏"),
    C2S_FIRE(3, "开炮"),
    C2S_HIT_FISH(4, "命中"),
    C2S_ADD_VALUE(5, "加炮"),
    C2S_SUB_VALUE(6, "减炮"),
    C2S_LIST_TASKS(7, "获取任务列表"),
    C2S_GET_REWARD(8, "领取奖励"),
    C2S_BUY_STORE_PAY(9, "商城购买钻石或金币"),
    C2S_BUY_STORE_ITEM(10, "商城钻石购买物品"),
    C2S_BUY_STORE_GOODS(11, "商城兑换物品"),
    C2S_BUY_MONTH_CARD(12, "购买月卡"),
    C2S_GET_AWARD_MONTH_CARD(13, "领取月卡奖励"),
    C2S_USE_ITEM(14, "使用道具"),
    C2S_GM_CMD(15, "GM命令"),
    C2S_GET_AWARD_VIP_PACK(16, "领取VIP礼包"),
    C2S_UNLOCK_BV(17, "解锁炮值"),
    C2S_USE_TITLE(18, "使用称号"),
    C2S_CHAT(19, "聊天"),
    C2S_READ_MAIL(20, "读取邮件"),
    C2S_GET_MAIL_APPENDIX(21, "获取邮件附件"),
    C2S_SELECT_BATTERY_SKIN(22, "选择炮台皮肤"),
    C2S_GET_ACHIEVEMENT_AWARD(23, "领取成就奖励"),
    C2S_LOCAL_BOMB_KILLFISH(24, "局部炸弹"),
    C2S_GET_ACTIVITY_AWARD(25, "活动奖励"),
    C2S_GET_ACTIVITY_INFO(26, "活动内容"),
    C2S_DEL_MAIL(27, "删除邮件"),
    C2S_READ_ITEM(28, "读取道具"),
    C2S_USE_BATTERY_SKILL(29, "使用炮台技能"),
    C2S_GET_STORE_ITEMS(30, "商城物品"),
    C2S_CHANGE_SCENE(31, "切场景"),
    C2S_READY(32, "场景完成"),
    C2S_LOCK_FISH(33, "锁定鱼"),
    C2S_UNLOCK(34, "取消锁定"),
    C2S_GET_SIGN_IN_AWARD(35, "一键领取奖励"),
    C2S_QUICK_ENTER(36, "快速进入"),
    C2S_GET_SEVEN_DAY_AWARD(37, "七日礼包"),
    C2S_GET_RELIEF_GOLD(38, "领取救济金"),
    C2S_BUY_SUPPORT_GIFT(39, "补给礼包"),
    C2S_BUY_MYSTERY_GIFT(40, "神秘礼包"),
    C2S_BUY_LIMIT_GIFT(41, "限购礼包"),
    C2S_SHOW_POP_NOTICE(42, "弹出公告"),
    C2S_QA_COMMIT(43, "投诉建议"),
    C2S_GET_ONLINE_GIFT(44, "在线奖励"),
    C2S_GET_REBATE_AWARD(45, "充值返利"),
    C2S_SET_SIGN(46, "设置签名"),
    C2S_QUIT_GAME(47, "退出游戏"),
    C2S_ACTIVITY_RANK_INFO(48, "活动排行"),
    C2S_IGNITE_TIME_BOMB(49, "引爆炸弹"),
    C2S_MAHJONG_EXCHANGE_AWARD(50, "麻将兑换"),
    C2S_USE_TINDER(51, "使用火种"),
    C2S_7DAY_GIFT(52, "7天奖励"),
    C2S_CHANGE_ROOM(53, "换桌"),
    C2S_RAND_MYSTERY(54, "神秘礼包"),
    C2S_HIT_EGG(55, "砸金蛋"),
    C2S_GET_EGG_GOLD_INFO(56, "金蛋信息"),
    C2S_REQ_STONE_STOCK(57, "灵石库存"),
    C2S_USE_CARD(58, "充值卡"),
    C2S_GET_NEWPGIFT_REWARD(59, "新手礼包奖励"),
    C2S_GET_NEWPGIFT_TASK(60, "新手任务奖励"),
    C2S_REAL_NAME(61, "实名认证"),
    C2S_BUY_CHRISMAS_GIFT(62, "圣诞礼包"),
    C2S_SET_LAST_OPT(63, "设置操作"),
    C2S_SET_HEAD(64, "设置头像"),
    C2S_NYR_DRAW(65, "新年红包"),
    C2S_NYW_EXCHANGE(66, "新年兑换"),
    C2S_NYC_GET_REWARD(67, "新春奖励"),
    C2S_BUY_BOMB_GIFT(68, "炸弹礼包"),
    C2S_SHOW_FIRST_TIP(69, "首次引导"),
    C2S_SELECT_INFO_BG(70, "信息背景"),
    C2S_PAY_RESULT(71, "支付结果"),
    C2S_USE_GUN_ADD_BET(72, "炮台倍率"),
    C2S_MAHJONG_EXCHANGE_AWARD_S(73, "麻将兑换S"),
    C2S_USE_TINDER_S(74, "火种S"),
    C2S_BIND_RECRUITER(75, "绑定邀请者"),
    C2S_BOMB_MONSTER(76, "炸怪兽"),
    C2S_DICING(77, "大富翁骰子"),
    C2S_FISHING_STACK_LIST_DATA(78, "捕鱼榜单"),
    C2S_BUY_ASSIST_PKG(79, "助力礼包"),
    C2S_LUCKY_CARD_REFRESH(80, "翻牌刷新"),
    C2S_LUCKY_CARD_START(81, "翻牌开始"),
    C2S_LUCKY_CARD_INVERT(82, "翻牌操作"),
    C2S_BUY_RMB_PKG(83, "RMB礼包"),
    C2S_GIVE_UP_RMB_PKG(84, "放弃礼包"),
    C2S_GET_VIP_INTEGRAL_TASK_AWARD(85, "贵族任务"),
    C2S_GET_DAILY_TASK_AWARD(86, "每日任务"),
    C2S_GET_WEEKLY_TASK_AWARD(87, "每周任务"),
    C2S_GET_PIGGY_BANK_GOLD(88, "存钱罐"),
    C2S_NEWER_WELFARE_LOTTERY(89, "新手抽奖"),
    C2S_BUY_DAY_CARD(90, "日卡"),
    C2S_TIMER_AWARD_GET(91, "定时奖励"),
    C2S_OPEN_AUTO_LOCK_TEST(92, "自动锁测试"),
    C2S_BIND_PROXY_ID(93, "绑定代理"),
    C2S_FUNC_FISH_COMMIT_DATA(94, "功能鱼提交"),
    C2S_FUNC_FISH_SKILL_INIT(95, "技能鱼初始化"),
    C2S_FUNC_FISH_SKILL_COMMIT_DATA(96, "技能鱼提交"),
    C2S_TEST_AUTO_ADD(97, "自动加属性"),
    C2S_BUY_GAODAM_PKG(98, "高达礼包"),
    C2S_CHANGE_BULLETVAL(99, "切炮值"),
    C2S_BUY_HELICOPTER_PKG(100, "直升机礼包"),
    C2S_TREASURE_IN_SEA_DRAW(101, "海底寻宝"),
    C2S_SNYC_FISH_POSITIONS(102, "同步鱼位置"),
    C2S_GET_ACCUM_RECHARGE_AWARD(103, "累计充值"),
    C2S_BUY_OVERVALUE_FUND(104, "超值基金"),
    C2S_REFRESH_NAVIGATION_TASK(105, "刷新任务"),
    C2S_GET_NAVIGATION_TASK_AWARD(106, "任务奖励"),
    C2S_ROOKIE_ACT_EXCHANGE_ITEM(107, "新手兑换"),
    C2S_BUY_NAVIGATION_PKG(108, "航海礼包"),
    C2S_GET_RELIEF_GOLD_NEW(109, "新救济金"),
    C2S_GET_MY_FISH_REWARD(110, "鱼奖励"),
    C2S_GET_FISH_POND_MSG_RECORD(111, "鱼池记录"),
    C2S_REQ_POPUP_CONFIG(112, "弹窗配置"),
    C2S_SYN_POPUP_INFO(113, "同步弹窗"),
    C2S_GET_SIGN_IN_AWARD_NEW(114, "签到new"),
    C2S_REPORT_AGE(115, "上报年龄"),
    C2S_BUY_MYSTERY_LEGEND_PKG(116, "秘境礼包"),
    C2S_SELECT_MYSTERY_ROOM(117, "选择秘境"),
    C2S_SHOW_LUCKY_CARD_ANIMATION(118, "动画完成"),
    C2S_BUY_DAILY_VALUE_SKILL_PKG(119, "每日技能包"),
    C2S_OUT_FISH_MYSTERY_LEGEND_DRAGON(120, "出神龙"),
    C2S_BUY_NEW_PLAYER_SEVEN_DAY_PKG(121, "七天礼包"),
    C2S_GIFT_PANGU(122, "盘古礼包"),
    C2S_RECORD_CHIYOU_DAILY_TASK(123, "蚩尤日常"),
    C2S_RECORD_CHIYOU_ITEM_TASK(124, "蚩尤道具"),
    C2S_RECORD_CHIYOU_RECHARGE_TASK(125, "蚩尤充值"),
    C2S_RECORD_CHIYOU_FISH_TASK(126, "蚩尤打鱼"),
    C2S_RECORD_CHIYOU_BAREBONE_OPEN(127, "宝骨"),
    C2S_RECORD_CHIYOU_EXCHANGE_AWARD(128, "蚩尤兑换"),
    C2S_SHANGGU_BLOOD_ADD_LEVEL(129, "上古血脉"),
    C2S_TAIGU_BLOOD_ADD_LEVEL(130, "太古血脉"),
    C2S_BREAKSKY_GOD_ACTIVATE(131, "开天神像"),
    C2S_BREAKSKY_GOD_GET(132, "神像奖励"),
    C2S_SHANHAI_ANIMAL_ADD_LEVEL(133, "山海卡牌"),
    C2S_SHANHAI_ANIMAL_GET_AWARD(134, "山海奖励"),
    C2S_OLD_GOD_BOX_GET(135, "古神匣"),
    C2S_SHANHAI_ANIMAL_ADD_DAY(136, "延长时间"),
    C2S_GET_BIG_KUN_DISC(137, "巨鲲结果"),
    C2S_GET_TPR_REWARD(138, "龟相奖励"),
    C2S_GET_LDK_REWARD(139, "龙王奖励"),
    C2S_BUY_STORE_PAY_BY_COUPONS(140, "点券购买"),
    C2S_BUY_STORE_PAY_FOR_COUPONS(141, "充值点券"),
    C2S_BUY_STORE_FREE_COUPONS(142, "免费点券"),
    C2S_NEW_YEAR_SIGN(143, "新春签到"),
    C2S_NEW_YEAR_SIGN_TOTAL(144, "累计签到"),
    C2S_NEW_YEAT_YAN_DI_GET_AWARD(145, "炎帝奖励"),
    C2S_YAN_DI_TREASURY_EXCHANGE(146, "炎帝兑换"),
    C2S_TIME_LIMIT_BURNOUT_REWARD(147, "限时奖励"),
    C2S_TIME_LIMIT_BURNOUT_REAL_REWARD(148, "实物奖励"),
    C2S_BUY_GIFT_PACK(149, "直购礼包"),
    C2S_USE_SKILL_NOT_USE_ITEM(150, "无道具技能"),
    C2S_BMC_LEADER_HEART(151, "上庄心跳"),
    C2S_FQZS_HEART(152, "飞禽心跳"),
    C2S_FUNC_FISH_COMMIT_DATA_NEW(153, "功能鱼新"),
    C2S_BMBC_HEART(154, "BMBC心跳"),
    C2S_PLAYER_PLACE(155, "玩家位置"),
    C2S_SHZ_EXIT_ROOM(156, "水浒退出"),
    C2S_RECONNECT_DESK(157, "重连房间"),
    C2S_BRNN_HEART(158, "牛牛心跳"),

    C2S_END(9999, "结束");

    private final int id;
    private final String desc;

    private static final java.util.Map<Integer, C2SMsgDef> MAP = new java.util.HashMap<>();

    static {
        for (C2SMsgDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated message ID  {}", def.id);
            }else {
                MAP.put(def.id, def);
            }
        }
    }

    C2SMsgDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static C2SMsgDef getById(int id) {
        return MAP.get(id);
    }
}
