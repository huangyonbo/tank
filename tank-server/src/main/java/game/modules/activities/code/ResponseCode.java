package game.modules.activities.code;


import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: King.Song
 * @Description:
 */
public enum ResponseCode {

    /**
     * 公共
     */
    Success(ResponseCodeType.None, 200, "成功"),
    SystemException(ResponseCodeType.None, 500, "系统异常"),
    Forbidden(ResponseCodeType.None, 403, "错误的请求参数"),
    UnLogin(ResponseCodeType.None, 999, "您的登录信息已失效,请重新登录!"),
    SingleLogin(ResponseCodeType.None, 700, "您账号已在其他设备登录,请重新登录!"),
    IllegalArgument(ResponseCodeType.Other, 2, "参数错误", "传入的参数(%s)不正确"),
    InLeaderNow(ResponseCodeType.None, 100001, "已经在队列中了", "当前位置(%s)"),
    NotHaveThisChess(ResponseCodeType.None, 100002, "不存在当前的这个Chess", "当前位置(%s)"),
    BombCoinNotEnough(ResponseCodeType.None, 100003, "无法下注,灵石不足", "当前位置(%s)"),
    NotInLeader(ResponseCodeType.None, 100004, "不在队列当中", "当前位置(%s)"),
    IsLeaderNow(ResponseCodeType.None, 100005, "当前是庄家，不能下注", "当前位置(%s)"),
    GameNotStart(ResponseCodeType.None, 100006, "当前不是下注的状态", "当前位置(%s)"),
    BombCoinNotEnoughToLeader(ResponseCodeType.None, 100007, "携带灵石不足2亿", "当前位置(%s)"),
    LeaderCountTen(ResponseCodeType.None, 100008, "坐庄次数大于10", "当前位置(%s)"),
    BombCoinNotEnoughContinueLeader(ResponseCodeType.None, 100009, "灵石小于5千万", "当前位置(%s)"),
    UnderLine(ResponseCodeType.None, 100010, "不在小游戏内", "当前位置(%s)"),
    StateRrror(ResponseCodeType.None, 100011, "请在本轮结束后下庄！", "当前位置(%s)"),
    AreaFull(ResponseCodeType.None, 100012, "此区域筹码上限，请更换筹码押注", "当前位置(%s)"),
    InOtherGame(ResponseCodeType.None, 100013, "在其他小游戏中", "当前位置(%s)"),
    RoomClose(ResponseCodeType.None, 100014, "该房间已关闭,请重新进入", "当前位置(%s)"),
    BounsCanNotPlay(ResponseCodeType.None, 100015, "无特殊游戏", "当前位置(%s)"),
    GoldNo(ResponseCodeType.None, 100016, "金额不足", "当前位置(%s)"),
    TooFrequently(ResponseCodeType.None, 100017, "操作频繁", "当前位置(%s)"),
    HasBeenInvited(ResponseCodeType.None, 100018, "已被邀请", "当前位置(%s)"),
    HasBeenCollected(ResponseCodeType.None, 100019, "已领取", "当前位置(%s)"),
    NotAvailableForCollectionAtPresent(ResponseCodeType.None, 100020, "暂时不可领取", "当前位置(%s)"),
    IN_OTHER_GAME(ResponseCodeType.None, 100021, "在其他游戏中", "当前位置(%s)"),
    CODE_ERROR(ResponseCodeType.None, 100022, "邀请码不存在", "当前位置(%s)"),

    ;


    private final static Map<Integer, ResponseCode> codeMap = Arrays.stream(ResponseCode.values()).collect(Collectors.toMap(ResponseCode::code, code -> code));

    private final int code;
    private String desc;
    private final String template;
    private final ResponseCodeType type;


    ResponseCode(ResponseCodeType type, int code, String desc, String template) {
        this.code = code;
        this.desc = desc;
        this.type = type;
        this.template = template;
    }

    ResponseCode(ResponseCodeType type, int code, String desc) {
        this.code = code;
        this.desc = desc;
        this.type = type;
        this.template = "";
    }

    public int code() {
        return this.code;
    }

    public String desc() {
        return this.desc;
    }

    public String template() {
        return this.template;
    }

    /**
     * 可自定义返回 desc
     *
     * @param desc
     * @return
     */
    public ResponseCode setDesc(String desc) {
        this.desc = desc;
        return this;
    }


    /**
     * @param code 代码
     * @return 转换出来的状态码
     */
    public static ResponseCode parse(Integer code) {
        return codeMap.get(code);
    }

    //    public String toJsonSting(){
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("code",this.code);
//        jsonObject.put("desc",this.desc);
//        return jsonObject.toString();
//    }
    public JSONResult toJSONResult() {
        JSONResult jsonObject = new JSONResult();
        jsonObject.put("code", this.code);
        jsonObject.put("desc", this.desc);
        return jsonObject;
    }


}
