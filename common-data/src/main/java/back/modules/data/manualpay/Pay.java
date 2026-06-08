package back.modules.data.manualpay;


import back.modules.data.ItemData;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 给玩家人工充值
 * Created by Administrator on 2018/5/15.
 */
public class Pay implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid;    //玩家ID
    private ItemData[] data = new ItemData[2];  //充值项

    public Pay() {
    }

    public Pay(int uid, String itemId, int price) {

        this.uid = uid;
        data[0] = new ItemData(itemId, 1);
        data[1] = new ItemData("item_charge_rmb", price);
    }

    public int getUid() {

        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public ItemData[] getData() {
        return data;
    }

    public void setData(ItemData[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Pay{" +
                "uid=" + uid +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
