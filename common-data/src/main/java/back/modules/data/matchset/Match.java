package back.modules.data.matchset;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/10/29.
 */
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //比赛id
    private int icon;//比赛icon
    private String name;    //比赛名
    private String channel;//渠道(1,2,3) [所有使用-1]
    private int type;//类型
    private int time;   //比赛时长[分钟]
    private String sign;    //报名消耗:Gold:1000,Diamond:1000
    private long begin; //开启时间
    private long over;  //结束时间
    private int maxPlayer;  //最大人数
    private int minPlayer;  //最低人数
    private int interval;   //间隔时间[分钟]
    private int protect;   //保护时间[分钟]
    private int settlement;   //结算时间[分钟]
    private boolean no1Tip; //第一名提示
    private String reward;//奖励(1-3:item_gold_1*1000;item_diamond_1*10|4-10:item_gold_1*1000;item_diamond_1*10|)
    private int coin;   //代币数
    private int maxBV;  //最大炮值
    private int minBV;  //最小炮值
    private int defBV;  //默认炮值
    private int offBV;  //炮值增量
    private String enterLimit;  //进入限制 Gold:1000,Diamond:1000
    private int maxCount;   //最大参与次数
    private String occupy;//占座 2,5,6
    private int maxTurn;    //最大开启次数

    public Match() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getOver() {
        return over;
    }

    public void setOver(long over) {
        this.over = over;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public void setMaxPlayer(int maxPlayer) {
        this.maxPlayer = maxPlayer;
    }

    public int getMinPlayer() {
        return minPlayer;
    }

    public void setMinPlayer(int minPlayer) {
        this.minPlayer = minPlayer;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getProtect() {
        return protect;
    }

    public void setProtect(int protect) {
        this.protect = protect;
    }

    public int getSettlement() {
        return settlement;
    }

    public void setSettlement(int settlement) {
        this.settlement = settlement;
    }

    public boolean isNo1Tip() {
        return no1Tip;
    }

    public void setNo1Tip(boolean no1Tip) {
        this.no1Tip = no1Tip;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public int getMaxBV() {
        return maxBV;
    }

    public void setMaxBV(int maxBV) {
        this.maxBV = maxBV;
    }

    public int getMinBV() {
        return minBV;
    }

    public void setMinBV(int minBV) {
        this.minBV = minBV;
    }

    public int getDefBV() {
        return defBV;
    }

    public void setDefBV(int defBV) {
        this.defBV = defBV;
    }

    public int getOffBV() {
        return offBV;
    }

    public void setOffBV(int offBV) {
        this.offBV = offBV;
    }

    public String getEnterLimit() {
        return enterLimit;
    }

    public void setEnterLimit(String enterLimit) {
        this.enterLimit = enterLimit;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getOccupy() {
        return occupy;
    }

    public void setOccupy(String occupy) {
        this.occupy = occupy;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getMaxTurn() {
        return maxTurn;
    }

    public void setMaxTurn(int maxTurn) {
        this.maxTurn = maxTurn;
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", icon=" + icon +
                ", name='" + name + '\'' +
                ", channel='" + channel + '\'' +
                ", type=" + type +
                ", time=" + time +
                ", sign='" + sign + '\'' +
                ", begin=" + begin +
                ", over=" + over +
                ", maxPlayer=" + maxPlayer +
                ", minPlayer=" + minPlayer +
                ", interval=" + interval +
                ", protect=" + protect +
                ", settlement=" + settlement +
                ", no1Tip=" + no1Tip +
                ", reward='" + reward + '\'' +
                ", coin=" + coin +
                ", maxBV=" + maxBV +
                ", minBV=" + minBV +
                ", defBV=" + defBV +
                ", offBV=" + offBV +
                ", enterLimit='" + enterLimit + '\'' +
                ", maxCount=" + maxCount +
                ", occupy='" + occupy + '\'' +
                ", maxTurn=" + maxTurn +
                '}';
    }
}
