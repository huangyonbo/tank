package back.modules.data.maintainset;


import java.io.Serializable;

/**
 * 关闭维护
 * Created by Administrator on 2018/4/11.
 */
public class StopMaintain implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;   //维护记录id

    public StopMaintain() {
    }

    public StopMaintain(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "StopMaintain{" +
                "id=" + id +
                '}';
    }
}
