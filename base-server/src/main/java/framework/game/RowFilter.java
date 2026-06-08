package framework.game;

import java.util.List;

public interface RowFilter {
    boolean check(int index,List<Object> objs);
}
