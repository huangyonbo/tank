package back.modules.data.gameset;

import back.modules.data.Pager;

/**
 * 游戏设置-桌数据查询
 * Created by Administrator on 2018/4/12.
 */
public class Desk extends Pager<DeskData> {
    private int gameType;   //游戏类型
    private int roomId; //厅类型

    public Desk() {
    }

    public Desk(int start, int limit, int gameType, int roomId) {
        super(start, limit);
        this.gameType = gameType;
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "Desk{" +
                "gameType=" + gameType +
                ", roomId=" + roomId +
                '}' +
                " " + super.toString();
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
}
