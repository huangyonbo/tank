package framework.mybatis.utils;

import java.util.List;

public class PageResult<T> {
    private List<T> records; // 当前页记录
    private long total;      // 总条数
    private long current;    // 当前页
    private long size;       // 每页条数

    // getter & setter
    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getCurrent() { return current; }
    public void setCurrent(long current) { this.current = current; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
