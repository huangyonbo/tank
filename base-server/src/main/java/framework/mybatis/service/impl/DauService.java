package framework.mybatis.service.impl;

import framework.mybatis.domain.Dau;
import framework.mybatis.mapper.DauMapper;
import org.springframework.stereotype.Service;

@Service
public class DauService extends AbstractServiceImpl<DauMapper, Dau> {

    public Dau queryById(Integer id) {
        return getById(id);
    }

    public Dau queryByUidDate(Integer uid, String date) {
        return lambdaQuery().eq(Dau::getUid,uid).eq(Dau::getDate,date).one();
    }
}
