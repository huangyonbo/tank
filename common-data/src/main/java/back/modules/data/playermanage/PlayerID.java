package back.modules.data.playermanage;


import java.io.Serializable;

/**
 * 玩家管理-封号、解封操作
 * Created by Administrator on 2018/4/25.
 */
public class PlayerID implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId; //玩家id

    public PlayerID() {
    }

    public PlayerID(int userId) {

        this.userId = userId;
    }

    public int getUserId() {

        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "PlayerID{" +
                "userId=" + userId +
                '}';
    }
}
