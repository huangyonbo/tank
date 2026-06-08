package framework.mybatis.service.impl;

import framework.mybatis.domain.ListHistory;
import framework.mybatis.mapper.ListHistoryMapper;
import org.springframework.stereotype.Service;

@Service
public class ListHistoryService extends AbstractServiceImpl<ListHistoryMapper, ListHistory> {

    public boolean addOne(ListHistory listHistory){
        return save(listHistory);
    }
}
