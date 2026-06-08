package back.modules.data.gameset;


import java.io.Serializable;

/**
 * 删除桌子
 * Created by Administrator on 2018/4/13.
 */
public class DeleteDesk implements Serializable {
    private static final long serialVersionUID = 1L;
    private int gameType;      //游戏类型
    private int id; //桌id

    public DeleteDesk(int gameType, int id) {
        this.gameType = gameType;
        this.id = id;
    }

    public DeleteDesk() {

    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DeleteDesk{" +
                "gameType=" + gameType +
                ", id=" + id +
                '}';
    }
}
