package back.modules.data.activitymanage;

import java.io.Serializable;
import java.util.Arrays;

public class ActivityParam implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer[] id;

    public ActivityParam() {
    }

    public ActivityParam(Integer[] id) {
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
        return "ActivityParam{" +
                "id=" + Arrays.deepToString(id) +
                '}';
    }
}
