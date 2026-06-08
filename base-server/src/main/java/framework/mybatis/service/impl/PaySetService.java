package framework.mybatis.service.impl;

import framework.mybatis.domain.PaySet;
import framework.mybatis.mapper.PaySetMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaySetService extends AbstractServiceImpl<PaySetMapper, PaySet> {

    public List<PaySet> loadAll() {
        return list();
    }
}
