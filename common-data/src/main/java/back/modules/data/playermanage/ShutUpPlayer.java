package back.modules.data.playermanage;


import java.io.Serializable;

/**
 * 玩家管理-禁言操作
 * Created by Administrator on 2018/4/25.
 */
public class ShutUpPlayer implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId; //玩家id
    private long type;   //禁言类型/时间[详见ShutUpType]

    public ShutUpPlayer() {
    }

    public ShutUpPlayer(int userId, long type) {

        this.userId = userId;
        this.type = type;
    }

    public int getUserId() {

        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ShutUpPlayer{" +
                "userId=" + userId +
                ", type=" + type +
                '}';
    }
}
