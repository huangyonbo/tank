package back.modules.data.runstats;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/16.
 */
public class RunStatsData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String createDate;
    private int roomType;   //房间类型
    private String serName; //服务器名称
    private long totalPlay; //总玩
    private long totalWin;  //总得

    public RunStatsData() {
    }

    public RunStatsData(String createDate, int roomType, String serName, long totalPlay, long totalWin) {

        this.createDate = createDate;
        this.roomType = roomType;
        this.serName = serName;
        this.totalPlay = totalPlay;
        this.totalWin = totalWin;
    }

    public String getCreateDate() {

        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public String getSerName() {
        return serName;
    }

    public void setSerName(String serName) {
        this.serName = serName;
    }

    public long getTotalPlay() {
        return totalPlay;
    }

    public void setTotalPlay(long totalPlay) {
        this.totalPlay = totalPlay;
    }

    public long getTotalWin() {
        return totalWin;
    }

    public void setTotalWin(long totalWin) {
        this.totalWin = totalWin;
    }

    @Override
    public String toString() {
        return "RunStatsData{" +
                "createDate='" + createDate + '\'' +
                ", roomType=" + roomType +
                ", serName='" + serName + '\'' +
                ", totalPlay=" + totalPlay +
                ", totalWin=" + totalWin +
                '}';
    }
}
