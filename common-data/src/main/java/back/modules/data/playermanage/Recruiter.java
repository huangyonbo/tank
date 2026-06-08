package back.modules.data.playermanage;

import back.modules.data.Pager;

public class Recruiter extends Pager<RecruiterData> {
    private int uid;

    public Recruiter() {
    }

    public Recruiter(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "Recruiter{" +
                "uid=" + uid +
                '}' +
                " " + super.toString();
    }
}
