package back.modules.data.player;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/5/14.
 */
public class SimplePlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;    //玩家ID
    private String username;    //玩家昵称
    private int level;  //等级
    private int vipLevel;   //VIP等级
    private int maxGun; //解锁炮值
    private long lottery;    //话费券

    public SimplePlayerData() {
    }

    public SimplePlayerData(int uid, String username, int level, int vipLevel) {

        this.uid = uid;
        this.username = username;
        this.level = level;
        this.vipLevel = vipLevel;
    }

    public SimplePlayerData(int uid, String username, int level, int vipLevel, int maxGun, long lottery) {
        this.uid = uid;
        this.username = username;
        this.level = level;
        this.vipLevel = vipLevel;
        this.maxGun = maxGun;
        this.lottery = lottery;
    }

    public int getUid() {

        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public int getMaxGun() {
        return maxGun;
    }

    public void setMaxGun(int maxGun) {
        this.maxGun = maxGun;
    }

    public long getLottery() {
        return lottery;
    }

    public void setLottery(long lottery) {
        this.lottery = lottery;
    }

    @Override
    public String toString() {
        return "SimplePlayerData{" +
                "uid=" + uid +
                ", username='" + username + '\'' +
                ", level=" + level +
                ", vipLevel=" + vipLevel +
                ", maxGun=" + maxGun +
                ", lottery=" + lottery +
                '}';
    }
}
