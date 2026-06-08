package back.modules.data.playermanage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2018/10/18.
 */
public class PutPlay implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;    //玩家id
    private List<PlayData> data;

    public PutPlay(int uid, List<PlayData> data) {
        this.uid = uid;
        this.data = data;
    }

    public PutPlay() {

    }

    public int getUid() {

        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public List<PlayData> getData() {
        return data;
    }

    public void setData(List<PlayData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PutPlay{" +
                "uid=" + uid +
                ", data=" + (data == null ? null : Arrays.deepToString(data.toArray())) +
                '}';
    }
}
