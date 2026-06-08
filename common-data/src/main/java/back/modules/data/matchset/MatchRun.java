package back.modules.data.matchset;

import back.modules.data.Pager;

import java.util.Arrays;

/**
 * Created by Administrator on 2018/10/29.
 */
public class MatchRun extends Pager<MatchRunData> {
    private Integer[] id; //比赛id

    public MatchRun(Integer[] id) {
        this.id = id;
    }

    public MatchRun() {

    }

    public Integer[] getId() {

        return id;
    }

    public void setId(Integer[] id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "MatchRun{" +
                "id=" + Arrays.toString(id) +
                '}' +
                " " + super.toString();
    }
}
