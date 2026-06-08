package back.modules.data.playermanage;

import back.modules.data.Pager;

/**
 *
 * Created by Administrator on 2018/4/23.
 */
public class RoomPlayer extends Pager<Integer> {
    private int id; //厅id

    public RoomPlayer() {
    }

    public RoomPlayer(int start, int limit, int id) {
        super(start, limit);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "RoomPlayer{" +
                "id=" + id +
                '}' + " " + super.toString();
    }
}
