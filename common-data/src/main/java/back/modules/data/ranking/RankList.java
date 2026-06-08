package back.modules.data.ranking;

import back.modules.data.Pager;

/**
 * Created by Administrator on 2018/8/31.
 */
public class RankList extends Pager<RankingData> {
    private String key; //榜单类型WeekList_Gold……

    public RankList() {
    }

    public RankList(String key) {

        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "RankList{" +
                "key='" + key + '\'' +
                '}';
    }
}
