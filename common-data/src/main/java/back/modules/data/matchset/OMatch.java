package back.modules.data.matchset;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/10/29.
 */
public class OMatch implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //比赛id

    public OMatch(int id) {
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
        return "OMatch{" +
                "id=" + id +
                '}';
    }
}
