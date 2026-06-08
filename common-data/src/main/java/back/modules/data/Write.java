package back.modules.data;

import back.modules.dataenum.Code;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/11.
 */
public class Write implements Serializable {
    private static final long serialVersionUID = 4L;
    private boolean success;
    private int code;
    private String msg;

    public Write() {
        success = true;
    }

    public Write(Code code, String msg) {
        this.success = false;
        this.code = code.getCode();
        this.msg = msg;
    }

    public Write(Code code) {
        this(code, "");
    }
    public Write SetMessage(String msg){
        this.msg=msg;
        return this;
    }

    public Write(String msg) {
        this(Code.FAIL, msg);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public Read getRead(){
        return new Read(success, code, msg);
    }
    @Override
    public String toString() {
        return "Write{" +
                "success=" + success +
                ", code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
