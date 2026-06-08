package back.modules.data.playermanage;

import back.modules.data.Pager;

import java.util.Arrays;

/**
 * 玩家管理-玩家列表
 * Created by Administrator on 2018/4/23.
 */
public class Player extends Pager<PlayerData> {

    private Integer[] id; //玩家id

    public Player() {
    }

    public Player(Integer[] id) {
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
        return "Player{" +
                "id=" + Arrays.toString(id) +
                '}' +
                " " + super.toString();
    }
}
