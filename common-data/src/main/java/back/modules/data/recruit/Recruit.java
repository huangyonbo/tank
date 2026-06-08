package back.modules.data.recruit;

import back.modules.data.Pager;

import java.util.Arrays;

/**
 * 好友招募
 * Created by 赵俊 on 2019/7/9.
 */
public class Recruit extends Pager<RecruitData> {
    private Integer[] id; //玩家id

    public Recruit() {
    }

    public Recruit(Integer[] id) {
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
        return "Recruit{" +
                "id=" + Arrays.toString(id) +
                '}' +
                " " + super.toString();
    }
}
