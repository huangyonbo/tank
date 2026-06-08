package back.modules.data.playermanage;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/10/18.
 */
public class PlayData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //房间id
    private long play;  //总玩
    private long win;   //总得

    public PlayData(int id, long play, long win) {
        this.id = id;
        this.play = play;
        this.win = win;
    }

    public PlayData() {

    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getPlay() {
        return play;
    }

    public void setPlay(long play) {
        this.play = play;
    }

    public long getWin() {
        return win;
    }

    public void setWin(long win) {
        this.win = win;
    }

    @Override
    public String toString() {
        return "PlayData{" +
                "id=" + id +
                ", play=" + play +
                ", win=" + win +
                '}';
    }
}
