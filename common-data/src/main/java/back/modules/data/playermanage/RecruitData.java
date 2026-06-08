package back.modules.data.playermanage;

import java.io.Serializable;

public class RecruitData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;
    private int weekRecharge;
    private long recruitTime;
    private int friendAmount;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getWeekRecharge() {
        return weekRecharge;
    }

    public void setWeekRecharge(int weekRecharge) {
        this.weekRecharge = weekRecharge;
    }

    public long getRecruitTime() {
        return recruitTime;
    }

    public void setRecruitTime(long recruitTime) {
        this.recruitTime = recruitTime;
    }

    public int getFriendAmount() {
        return friendAmount;
    }

    public void setFriendAmount(int friendAmount) {
        this.friendAmount = friendAmount;
    }

    @Override
    public String toString() {
        return "RecruitData{" +
                "uid=" + uid +
                ", weekRecharge=" + weekRecharge +
                ", recruitTime=" + recruitTime +
                ", friendAmount=" + friendAmount +
                '}';
    }
}
