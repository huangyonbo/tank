package back.modules.data.gameset;

import back.modules.data.Pager;

/**
 * 游戏设置-厅数据查询
 * Created by Administrator on 2018/4/12.
 */
public class Room extends Pager<RoomData> {
    private int gameType;   //游戏类型

    public Room() {
    }

    public Room(int gameType) {
        super();
        this.gameType = gameType;
    }

    @Override
    public String toString() {
        return "Room{" +
                "gameType=" + gameType +
                '}' +
                " " + super.toString();
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }
}
