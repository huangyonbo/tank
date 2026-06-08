package back.modules.data.recruit;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/23.
 */
public class RecruitData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;
    private String username;
    private int level;
    private int vipLevel;
    private int bulletLevel;
    private int channel;
    private long totalRechargeAmount;
    private long weekRecharge;
    private String shareCode;
    private int friendCount;
    private int exchangeCard;
    private int voucher;

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

    public int getBulletLevel() {
        return bulletLevel;
    }

    public void setBulletLevel(int bulletLevel) {
        this.bulletLevel = bulletLevel;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public long getTotalRechargeAmount() {
        return totalRechargeAmount;
    }

    public void setTotalRechargeAmount(long totalRechargeAmount) {
        this.totalRechargeAmount = totalRechargeAmount;
    }

    public long getWeekRecharge() {
        return weekRecharge;
    }

    public void setWeekRecharge(long weekRecharge) {
        this.weekRecharge = weekRecharge;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(int friendCount) {
        this.friendCount = friendCount;
    }

    public int getExchangeCard() {
        return exchangeCard;
    }

    public void setExchangeCard(int exchangeCard) {
        this.exchangeCard = exchangeCard;
    }

    public int getVoucher() {
        return voucher;
    }

    public void setVoucher(int voucher) {
        this.voucher = voucher;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "RecruitData{" +
                "username='" + username + '\'' +
                ", level=" + level +
                ", uid=" + uid +
                ", vipLevel=" + vipLevel +
                ", bulletLevel=" + bulletLevel +
                ", channel=" + channel +
                ", totalRechargeAmount=" + totalRechargeAmount +
                ", weekRecharge=" + weekRecharge +
                ", shareCode='" + shareCode + '\'' +
                ", friendCount=" + friendCount +
                ", exchangeCard=" + exchangeCard +
                ", voucher=" + voucher +
                '}';
    }
}
