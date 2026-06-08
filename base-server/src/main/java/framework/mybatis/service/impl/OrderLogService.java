package framework.mybatis.service.impl;

import framework.mybatis.domain.OrderLog;
import framework.mybatis.mapper.OrderLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderLogService extends AbstractServiceImpl<OrderLogMapper, OrderLog> {

    public boolean addOne(OrderLog log){
        return save(log);
    }

    public OrderLog queryById(Integer id) {
        return getById(id);
    }

    public List<OrderLog> queryByType(Integer type) {
        return lambdaQuery().eq(OrderLog::getType,type).orderByAsc(OrderLog::getCreateTime).last(" limit 0, 50").list();
    }
}
