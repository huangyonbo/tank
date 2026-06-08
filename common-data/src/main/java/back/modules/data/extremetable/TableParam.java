package back.modules.data.extremetable;

import java.io.Serializable;

public class TableParam implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;

    public TableParam() {
    }

    public TableParam(int id) {
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
        return "TableParam{" +
                "id=" + id +
                '}';
    }
}
