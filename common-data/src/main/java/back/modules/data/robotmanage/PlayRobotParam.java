package back.modules.data.robotmanage;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/9/1.
 */
public class PlayRobotParam implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;

    public PlayRobotParam() {
    }

    public PlayRobotParam(int uid) {
        this.uid = uid;
    }

    public int getUid() {

        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "PlayRobotParam{" +
                "uid=" + uid +
                '}';
    }
}
