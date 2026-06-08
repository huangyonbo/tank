package framework.mybatis.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.function.Function;

public class PageUtils {
    /**
     * 通用分页查询工具方法
     * @param currentPage 当前页码，从1开始
     * @param pageSize 每页条数
     * @param queryWrapper 查询条件
     * @param queryFunc 数据查询函数，接收 Page 和 LambdaQueryWrapper
     * @param <T> 实体类型
     * @return 分页结果
     */
    public static <T> PageResult<T> queryPage(long currentPage,
                                              long pageSize,
                                              LambdaQueryWrapper<T> queryWrapper,
                                              Function<Page<T>, IPage<T>> queryFunc) {
        Page<T> page = new Page<>(currentPage, pageSize);

        // 执行查询
        IPage<T> resultPage = queryFunc.apply(page);

        // 封装返回结果
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setRecords(resultPage.getRecords());
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setCurrent(currentPage);
        pageResult.setSize(pageSize);

        return pageResult;
    }
}
