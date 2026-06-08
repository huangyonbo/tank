package back.modules.data.playermanage;

import java.io.Serializable;

public class RecruiterData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int recruiter;
    private int friendAmount;

    public int getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(int recruiter) {
        this.recruiter = recruiter;
    }

    public int getFriendAmount() {
        return friendAmount;
    }

    public void setFriendAmount(int friendAmount) {
        this.friendAmount = friendAmount;
    }

    @Override
    public String toString() {
        return "RecruiterData{" +
                "recruiter=" + recruiter +
                ", friendAmount=" + friendAmount +
                '}';
    }
}
