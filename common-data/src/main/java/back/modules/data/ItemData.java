package back.modules.data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/5/4.
 */
public class ItemData implements Serializable {
    private static final long serialVersionUID = 2L;
    private String id; //物品id
    private int count;  //物品数量

    public ItemData() {
    }

    public ItemData(String id, int count) {
        this.id = id;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "ItemData{" +
                "id='" + id + '\'' +
                ", count=" + count +
                '}';
    }
}
