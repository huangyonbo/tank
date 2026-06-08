package back.modules.data.gameset;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/12.
 */
public class RoomData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //厅类型Id[详见RoomType]
    private int online; //在线人数
    private double totalPlay;   //总玩
    private double totalGet;    //总得
    private double totalWin;    //总赢
    private int minGun; //最小炮值
    private int maxGun; //最大炮值
    private int vipLevel;   //VIP等级
    private int autoKick;   //自动踢出

    public RoomData() {
    }

    public RoomData(int id, int online, double totalPlay, double totalGet, double totalWin, int minGun, int maxGun, int vipLevel, int autoKick) {

        this.id = id;
        this.online = online;
        this.totalPlay = totalPlay;
        this.totalGet = totalGet;
        this.totalWin = totalWin;
        this.minGun = minGun;
        this.maxGun = maxGun;
        this.vipLevel = vipLevel;
        this.autoKick = autoKick;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getMinGun() {
        return minGun;
    }

    public void setMinGun(int minGun) {
        this.minGun = minGun;
    }

    public int getMaxGun() {
        return maxGun;
    }

    public void setMaxGun(int maxGun) {
        this.maxGun = maxGun;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public int getAutoKick() {
        return autoKick;
    }

    public void setAutoKick(int autoKick) {
        this.autoKick = autoKick;
    }

    @Override
    public String toString() {
        return "RoomData{" +
                "id=" + id +
                ", online=" + online +
                ", totalPlay=" + totalPlay +
                ", totalGet=" + totalGet +
                ", totalWin=" + totalWin +
                ", minGun=" + minGun +
                ", maxGun=" + maxGun +
                ", vipLevel=" + vipLevel +
                ", autoKick=" + autoKick +
                '}';
    }
}
