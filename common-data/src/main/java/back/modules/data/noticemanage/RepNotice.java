package back.modules.data.noticemanage;


import java.io.Serializable;

/**
 * 撤销公告
 * Created by Administrator on 2018/4/18.
 */
public class RepNotice implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //公告记录id
    private int type;   //公告类型[详见NoticeType]

    public RepNotice() {
    }

    public RepNotice(int id, int type) {
        this.id = id;
        this.type = type;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RepNotice{" +
                "id=" + id +
                "type=" + type +
                '}';
    }
}
