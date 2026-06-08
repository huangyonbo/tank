package back.modules.data.gameset;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/12.
 */
public class DeskData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //桌Id
    private String name;    //桌名
    private int roomId; //所属厅id
    private int online; //在线人数
    private double totalPlay;   //总玩
    private double totalGet;    //总得
    private double totalWin;    //总赢

    @Override
    public String toString() {
        return "DeskData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", roomId=" + roomId +
                ", online=" + online +
                ", totalPlay=" + totalPlay +
                ", totalGet=" + totalGet +
                ", totalWin=" + totalWin +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public double getTotalPlay() {
        return totalPlay;
    }

    public void setTotalPlay(double totalPlay) {
        this.totalPlay = totalPlay;
    }

    public double getTotalGet() {
        return totalGet;
    }

    public void setTotalGet(double totalGet) {
        this.totalGet = totalGet;
    }

    public double getTotalWin() {
        return totalWin;
    }

    public void setTotalWin(double totalWin) {
        this.totalWin = totalWin;
    }
}
