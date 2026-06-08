package back.modules.data.paymanage;


import back.modules.data.ItemData;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 充值管理-补单操作
 * Created by Administrator on 2018/4/26.
 */
public class ResupplyPay implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uid; //玩家id
    private ItemData[] data = new ItemData[2];    //补单项

    public ResupplyPay() {
    }

    public ResupplyPay(int uid, String itemId, int price) {
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
        return "ResupplyPay{" +
                "uid=" + uid +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
