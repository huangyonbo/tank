package back.modules.data.playermanage;

import back.modules.data.Pager;

public class Recruit extends Pager<RecruitData> {
    private int uid;
    private long startTime;
    private long endTime;

    public Recruit() {
    }

    public Recruit(int uid, long startTime, long endTime) {
        this.uid = uid;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "Recruit{" +
                "uid=" + uid +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}' +
                " " + super.toString();
    }
}
