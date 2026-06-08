package back.modules.data.robotmanage;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/31.
 */
public class CloseRank implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;
    private String proName; //哪种属性榜
    private boolean day;    //日榜
    private boolean week;   //周榜
    private boolean month;   //月榜
    private boolean total;   //总榜

    public CloseRank() {
    }

    public CloseRank(int uid, String proName) {
        this.uid = uid;
        this.proName = proName;
        this.day = false;
        this.week = true;
        this.month = false;
        this.total = false;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
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
        return "CloseRank{" +
                "uid=" + uid +
                ", proName='" + proName + '\'' +
                ", day=" + day +
                ", week=" + week +
                ", month=" + month +
                ", total=" + total +
                '}';
    }
}
