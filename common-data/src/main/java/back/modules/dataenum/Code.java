package back.modules.dataenum;

/**
 * 错误码
 * Created by Administrator on 2018/5/12.
 */
public enum Code {
    FAIL(2000),
    NOT_EXIST(2001),    //不存在
    LACK(2002); //物品不足

    private final int code;

    private Code(final int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
