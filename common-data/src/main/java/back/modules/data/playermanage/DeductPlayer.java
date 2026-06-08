package back.modules.data.playermanage;


import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 玩家管理-扣除操作
 * Created by Administrator on 2018/4/25.
 */
public class DeductPlayer implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId; //玩家id
    private List<DeductPlayerData> data;

    public DeductPlayer() {
    }

    public DeductPlayer(int userId, List<DeductPlayerData> data) {

        this.userId = userId;
        this.data = data;
    }

    public int getUserId() {

        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<DeductPlayerData> getData() {
        return data;
    }

    public void setData(List<DeductPlayerData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DeductPlayer{" +
                "userId=" + userId +
                ", data=" + Arrays.deepToString(data.toArray()) +
                '}';
    }
}
