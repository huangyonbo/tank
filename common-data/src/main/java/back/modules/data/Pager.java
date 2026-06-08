package back.modules.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询数据
 * Created by ZhaoJun on 2018/4/11.
 */
public class Pager<T> implements Serializable {
    private static final long serialVersionUID = 3L;
    private boolean page;   //是否分页 true-分页 false-不分页
    private int start;  //本页第一个数据位置
    private int limit;  //本页数据量
    private int total;  //数据总量
    private List<T> root = new ArrayList<>();  //数据详情

    public Pager() {
        this.page = false;
    }

    public Pager(int start, int limit){
        this.page = true;
        this.start = start;
        this.limit = limit;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.page = true;
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.page = true;
        this.limit = limit;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getRoot() {
        return root;
    }

    public void setRoot(List<T> root) {
        this.root = root;
    }

    public boolean isPage() {
        return page;
    }

    public void setPage(boolean page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "Pager{" +
                "start=" + start +
                ", limit=" + limit +
                ", total=" + total +
                ", page=" + page +
                ", root=" + root.size() +
                '}';
    }
}
