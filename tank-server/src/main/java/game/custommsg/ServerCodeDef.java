package game.custommsg;

/**
 *
 * 描述：
 * 文件：ServerCode.java
 * 创建人：胡中伟
 * 创建时间：2018年4月17日 下午5:04:37
 *
 */
public enum ServerCodeDef implements MsgDef{
    CODE_SUCCESS(0, ""),
    CODE_LEVEL_MAX(1, "已经满级"),
    CODE_NEED_PRO(2, "属性不足"),
    CODE_NEED_ITEM(3, "道具不足"),
    CODE_UID_NOT_EXIST(4, "玩家不存在"),
    CODE_COUNT_ILLEGAL(5, "数量非法"),
    CODE_PARAM_ERR(6, "参数错误"),
    CODE_NOT_ENOUGH(7, "需要的数据不足"),
    CODE_FAILED(8, "失败"),
    CODE_IN_ROOM(9, "已经在房间"),
    CODE_ROOM_TYPE_ERR(10, "房间类型错误"),
    CODE_VIP_LIMIT(11, "VIP限制"),
    CODE_BV_LIMIT(12, "炮值限制"),
    CODE_CANT_SEND(13, "道具不可赠送"),
    CODE_NOT_EXIST(14, "道具不存在"),
    CODE_CANT_USE(15, ""),
    CODE_NOT_IN_GAME(16, "不在游戏内"),
    CODE_MATRI_NOT_ENOUGH(17, ""),
    CODE_TIMES_LIMIT(18, "次数限制"),
    CODE_NO_STOCK(19, "库存不足"),
    CODE_WRONG_GAMEID(20, "比赛ID无效"),
    CODE_WRONG_CHANNEL(21, "渠道错误"),
    CODE_WRONG_STATE(22, "状态错误"),
    CODE_PROTECTED(23, "保护中"),
    CODE_CON_LIMIT(24, "条件限制"),
    CODE_SIGN_LIMIT(25, "报名费限制"),
    CODE_WRONG_TYPE(26, "类型无效"),
    CODE_ROOM_FULL(27, "房间已满"),
    CODE_SEAT_FULL(28, "座位已满"),
    CODE_WRONG_PASSWD(29, "密码错误"),
    CODE_NOT_IN_ARENA(30, "不在竞技场内"),
    CODE_IN_USE(31, "被占用"),
    CODE_PASSWD(32, "密码重复"),
    CODE_COUNT_LIMIT(33, "数量限制"),
    CODE_IS_DIED(34, "已死"),
    CODE_TIME_LIMIT(35, "时间限制"),
    CODE_NUMBER_LIMIT(36, "数量限制"),
    CODE_CANT_RECYCLE(37, "道具不可回收"),
    CODE_MAX_GOLD_LIMIT(38, ""),
    CODE_PROXY_DIFFERENT(39, "代理不一致"),
    CODE_IN_ENTER_ROOM_CD(40, "在切换房间CD中"),
    CODE_IN_OTHER_GAME(41, "在其他游戏中"),
    CODE_NOT_VIP_NOT_SEND(42, "非vip只能赠送上级"),
    CODE_DESK_NOT_FUND_PW(43, "密码桌子没有发现"),

    CODE_END(Integer.MAX_VALUE, "");

    private final int id;
    private final String desc;

    private ServerCodeDef(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }
}
