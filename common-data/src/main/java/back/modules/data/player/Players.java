package back.modules.data.player;

import back.modules.data.Pager;

import java.util.Arrays;

/**
 * 公用查询部分玩家数据
 * Created by Administrator on 2018/5/21.
 */
public class Players extends Pager<SimplePlayerData> {
    private Integer[] id; //玩家id

    public Players() {
    }

    public Players(Integer[] id) {
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
        return "Players{" +
                "id=" + Arrays.toString(id) +
                '}' +
                " " + super.toString();
    }
}
