package back.modules.data.robotmanage;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/31.
 */
public class OpenRank implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;
    private String name;    //昵称
    private String proName; //哪种属性榜
    private long value; //数值
    private int vip;    //VIP等级
    private String title;   //称号
    private int level;  //账号等级
    private boolean day;    //日榜
    private boolean week;   //周榜
    private boolean month;   //月榜
    private boolean total;   //总榜

    public OpenRank() {
    }

    public OpenRank(int uid, String name, String proName, long value, int vip, String title, int level) {
        this.uid = uid;
        this.name = name;
        this.proName = proName;
        this.value = value;
        this.vip = vip;
        this.title = title;
        this.level = level;
        this.week = true;
        this.day = false;
        this.month = false;
        this.total = false;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public int getVip() {
        return vip;
    }

    public void setVip(int vip) {
        this.vip = vip;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isDay() {
        return day;
    }

    public void setDay(boolean day) {
        this.day = day;
    }

    public boolean isWeek() {
        return week;
    }

    public void setWeek(boolean week) {
        this.week = week;
    }

    public boolean isMonth() {
        return month;
    }

    public void setMonth(boolean month) {
        this.month = month;
    }

    public boolean isTotal() {
        return total;
    }

    public void setTotal(boolean total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "OpenRank{" +
                "uid=" + uid +
                ", name='" + name + '\'' +
                ", proName='" + proName + '\'' +
                ", value=" + value +
                ", vip=" + vip +
                ", title='" + title + '\'' +
                ", level=" + level +
                ", day=" + day +
                ", week=" + week +
                ", month=" + month +
                ", total=" + total +
                '}';
    }
}
