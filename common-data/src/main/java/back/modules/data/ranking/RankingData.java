package back.modules.data.ranking;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/5/5.
 */
public class RankingData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int rank;   //排名
    private String name;    //玩家昵称
    private int uid;    //玩家ID
    private long value; //数额

    public RankingData() {
    }

    public RankingData(int rank, String name, int uid, long value) {

        this.rank = rank;
        this.name = name;
        this.uid = uid;
        this.value = value;
    }

    public int getRank() {

        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RankingData{" +
                "rank=" + rank +
                ", name='" + name + '\'' +
                ", uid=" + uid +
                ", value=" + value +
                '}';
    }
}
