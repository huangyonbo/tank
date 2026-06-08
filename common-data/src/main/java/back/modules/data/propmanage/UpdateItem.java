package back.modules.data.propmanage;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/10/18.
 */
public class UpdateItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;//物品id
    private int stock;//库存
    private int limit;  //限购次数
    private boolean dayReset;   //每日重置

    public UpdateItem(String id, int stock, int limit, boolean dayReset) {
        this.id = id;
        this.stock = stock;
        this.limit = limit;
        this.dayReset = dayReset;
    }

    public UpdateItem() {

    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isDayReset() {
        return dayReset;
    }

    public void setDayReset(boolean dayReset) {
        this.dayReset = dayReset;
    }

    @Override
    public String toString() {
        return "UpdateItem{" +
                "id='" + id + '\'' +
                ", stock=" + stock +
                ", limit=" + limit +
                ", dayReset=" + dayReset +
                '}';
    }
}
