package back.modules.data.mailmanage;


import java.io.Serializable;

/**
 * Created by Administrator on 2018/5/10.
 */
public class ItemValue implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;  //物品ID
    private boolean exist;  //该ID是否存在
    private float value;  //若存在，则单个物品价值（元）

    public ItemValue() {
    }

    public ItemValue(String id, float value) {
        this.id = id;
        this.exist = true;
        this.value = value;
    }

    public ItemValue(String id) {
        this.id = id;
        this.exist = false;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ItemValue{" +
                "id='" + id + '\'' +
                ", exist=" + exist +
                ", value=" + value +
                '}';
    }
}
