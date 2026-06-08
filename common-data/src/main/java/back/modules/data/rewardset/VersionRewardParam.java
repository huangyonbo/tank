package back.modules.data.rewardset;

import java.io.Serializable;
import java.util.Arrays;

public class VersionRewardParam implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer[] id;

    public VersionRewardParam() {
    }

    public VersionRewardParam(Integer[] id) {
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
        return "VersionRewardParam{" +
                "id=" + Arrays.deepToString(id) +
                '}';
    }
}
