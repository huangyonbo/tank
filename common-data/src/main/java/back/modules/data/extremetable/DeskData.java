package back.modules.data.extremetable;

import java.io.Serializable;

public class DeskData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int deskId;
    private int seatId;
    private int uid;
    private long totalPlay;
    private long totalWin;
    private int vipLevel;
    private long mojin;
    private int sanchaji1;
    private int sanchaji2;
    
    public int getVipLevel() {
		return vipLevel;
	}

	public void setVipLevel(int vipLevel) {
		this.vipLevel = vipLevel;
	}

	public long getMojin() {
		return mojin;
	}

	public void setMojin(long mojin) {
		this.mojin = mojin;
	}

	public int getSanchaji1() {
		return sanchaji1;
	}

	public void setSanchaji1(int sanchaji1) {
		this.sanchaji1 = sanchaji1;
	}

	public int getSanchaji2() {
		return sanchaji2;
	}

	public void setSanchaji2(int sanchaji2) {
		this.sanchaji2 = sanchaji2;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int getDeskId() {
        return deskId;
    }

    public void setDeskId(int deskId) {
        this.deskId = deskId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
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
        return "DeskData{" +
                "deskId=" + deskId +
                ", seatId=" + seatId +
                ", uid=" + uid +
                ", totalPlay=" + totalPlay +
                ", totalWin=" + totalWin +
                '}';
    }
}
