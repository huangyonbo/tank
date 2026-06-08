package back.modules.data.rewardset;

import java.io.Serializable;
import java.util.Arrays;

public class DailyRewardParam implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer[] id;

    public DailyRewardParam() {
    }

    public DailyRewardParam(Integer[] id) {
        this.id = id;
    }

    public Integer[] getId() {
        return id;
    }

    public void setId(Integer[] id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DailyRewardParam{" +
                "id=" + Arrays.deepToString(id) +
                '}';
    }
}
