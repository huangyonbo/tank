package back.modules.data.blacklist;


import java.io.Serializable;

/**
 * 黑名单信息
 */
public class BlackOperate implements Serializable {
    private static final long serialVersionUID = 1L;
    private int type;   //封禁/解封类型[详见FreezeType]
    private String text;    //封禁/解封内容

    public BlackOperate() {
    }

    public BlackOperate(int type, String text) {

        this.type = type;
        this.text = text;
    }

    public int getType() {

        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "BlackOperate{" +
                "type=" + type +
                ", text='" + text + '\'' +
                '}';
    }
}
