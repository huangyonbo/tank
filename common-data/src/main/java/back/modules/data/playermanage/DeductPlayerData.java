package back.modules.data.playermanage;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/4/25.
 */
public class DeductPlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id; //物品id
    private int count;  //物品数量

    public DeductPlayerData() {
    }

    public DeductPlayerData(String id, int count) {
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
        return "DeductPlayerData{" +
                "id='" + id + '\'' +
                ", count=" + count +
                '}';
    }
}
