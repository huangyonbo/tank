package framework.mybatis.service.impl;

import framework.mybatis.domain.Advice;
import framework.mybatis.mapper.AdviceMapper;
import org.springframework.stereotype.Service;

@Service
public class AdviceService extends AbstractServiceImpl<AdviceMapper, Advice> {

    public Advice queryById(Integer id) {
        return getById(id);
    }
}
