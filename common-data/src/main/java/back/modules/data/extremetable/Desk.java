package back.modules.data.extremetable;

import back.modules.data.Pager;

public class Desk extends Pager<DeskData> {
    private int deskId;
    private int deskType;

    public Desk() {
    }

    public Desk(int deskId, int deskType) {
        this.deskId = deskId;
        this.deskType = deskType;
    }

    public int getDeskId() {
        return deskId;
    }

    public void setDeskId(int deskId) {
        this.deskId = deskId;
    }

    public int getDeskType() {
        return deskType;
    }

    public void setDeskType(int deskType) {
        this.deskType = deskType;
    }

    @Override
    public String toString() {
        return "Desk{" +
                "deskId=" + deskId +
                ", deskType=" + deskType +
                '}' +
                " " + super.toString();
    }
}
