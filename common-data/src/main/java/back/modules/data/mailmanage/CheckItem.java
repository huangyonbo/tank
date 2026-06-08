package back.modules.data.mailmanage;

import back.modules.data.Pager;

import java.util.Arrays;

/**
 * 校验物品有效性
 * Created by Administrator on 2018/5/10.
 */
public class CheckItem extends Pager<ItemValue> {
    private String[] item;    //物品id

    public CheckItem() {
    }

    public CheckItem(String[] item) {
        this.item = item;
    }

    public String[] getItem() {

        return item;
    }

    public void setItem(String[] item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "CheckItem{" +
                "item=" + Arrays.toString(item) +
                '}' +
                " " + super.toString();
    }
}
