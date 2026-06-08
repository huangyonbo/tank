package back.modules.data.playermanage;

import back.modules.data.Pager;

/**
 * Created by Administrator on 2018/10/18.
 */
public class GetPlay extends Pager<PlayData> {
    private int uid;    //玩家id

    public GetPlay() {
    }

    public GetPlay(int uid) {
        this.uid = uid;
    }

    public int getUid() {

        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "GetPlay{" +
                "uid=" + uid +
                '}' +
                " " + super.toString();
    }
}
