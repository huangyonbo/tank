package back.modules.data.gameset;


import java.io.Serializable;
import java.util.Arrays;

/**
 * 新增桌子
 * Created by Administrator on 2018/4/13.
 */
public class CreateDesk implements Serializable {
    private static final long serialVersionUID = 1L;
    private int gameType;      //游戏类型
    private int roomType;   //厅类型[详见RoomType]
    private String[] name; //桌名

    public CreateDesk() {
    }

    public CreateDesk(int gameType, int roomType, String[] name) {
        this.gameType = gameType;
        this.roomType = roomType;
        this.name = name;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CreateDesk{" +
                "gameType=" + gameType +
                ", roomType=" + roomType +
                ", name=" + Arrays.toString(name) +
                '}';
    }
}
