package game.custommsg;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public enum CommandDef implements MsgDef{
    CMD_TEST(0, "测试"),
    CMD_TASK_VIEW(1, "任务查看"),
    CMD_FISH_DIE(2, "鱼死亡"),// 参数：player, fishindex, fishcfg
    CMD_USE_ITEM(3, "使用道具"),// 参数：player, count
    CMD_CLIENT_READY(4, "客户端准备"),// 参数：player
    CMD_PAY_BACK(5, "支付回调"),// 参数：goodsId, payMoney, info
    CMD_KILL_ONE_FISH(6, "击杀单条鱼"),// 参数：fishcfg, gold
    CMD_DROP_DICE(7, "投骰子"),// 参数：itemId
    CMD_DROP_DICE_S(8, "投骰子S"),// 参数：itemId
    CMD_BUY_MYSTERY(9, "购买神秘奖励"),// 参数：base, rand
    CMD_ACT_START(10, "活动开始"),// 参数：cfg
    CMD_ACT_STOP(11, "活动结束"),// 参数：cfg
    CMD_ACT_START_SHOW(12, "活动展示开始"),// 参数：cfg
    CMD_ACT_STOP_SHOW(13, "活动展示结束"),// 参数：cfg
    CMD_CHANGE_DAY(14, "切天"),
    CMD_REFRESH_LIST(15, "刷新列表"),
    CMD_CHECK_MAIL(16, "检查邮件"),
    CMD_CLEAR_RECRUIT(17, "清除招募"),
    CMD_CLEAR_BOMB_2_BOMBCOIN(18, "清除炸弹转金币"),
    CMD_KILL_MONSTER(19, "击杀怪兽"),
    CMD_MONSTER_AWARD_RECORD(20, "怪兽奖励记录"),
    CMD_GET_ITEM(21, "获得道具"),
    CMD_STONE_EXCHANGE(22, "灵石兑换"),
    CMD_CHANGE_WEEK(23, "切周"),
    CMD_HIT_FISH(24, "击中鱼"),
    CMD_PIGGY_BANK_STOP(25, "存钱罐停止"),
    CMD_FISH_OUT_OF_TIME(26, "鱼超时"),
    CMD_FUNC_FISH_SKILL_INIT(27, "功能鱼技能初始化"),
    CMD_GM_OUT_FISH(28, "GM刷鱼"),
    CMD_GM_OUT_FISH_GROUP(29, "GM刷鱼组"),
    CMD_REG_SUCCESS(30, "注册成功"),
    CMD_ACT_REFRESH(31, "刷新活动"),
    CMD_SUB_PROPERTY_ITEM(32, "扣除属性道具"),
    CMD_USE_SKILL(33, "使用技能"),
    CMD_CHECK_SEND_ITEMS(34, "检测赠送道具"),

    CMD_END(9999, "结束");

    private final int id;
    private final String desc;

    private static final Map<Integer, CommandDef> MAP = new HashMap<>();

    static {
        for (CommandDef def : values()) {
            if (MAP.containsKey(def.id)) {
                log.error("Repeated message ID  {}", def.id);
            }else {
                MAP.put(def.id, def);
            }
        }
    }

    CommandDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public static CommandDef getById(int id) {
        return MAP.get(id);
    }
}
