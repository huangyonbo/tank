package back.modules.data.ranking;

import back.modules.data.Pager;

/**
 * 排行榜统计
 * Created by Administrator on 2018/5/5.
 */
public class Ranking extends Pager<RankingData> {
    private int type;   //排行榜类型[详见RankingType]

    public Ranking() {
    }

    public Ranking(int start, int limit, int type) {

        super(start, limit);
        this.type = type;
    }

    public int getType() {

        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Ranking{" +
                "type=" + type +
                '}' +
                " " + super.toString();
    }
}
