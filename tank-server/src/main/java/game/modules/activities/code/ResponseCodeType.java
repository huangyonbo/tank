package game.modules.activities.code;

public enum ResponseCodeType {
    None(0, "无"),
    System(50, "系统"),
    Other(55, "其他"),
    User(61, "用户玩家"),
    Game(71, "游戏"),
    Play(81, "玩游戏")
    ;

    private final int code;
    private final String desc;

    ResponseCodeType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() {
        return this.code;
    }

    public String desc() {
        return this.desc;
    }
}
