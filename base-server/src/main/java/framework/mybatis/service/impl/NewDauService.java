package framework.mybatis.service.impl;

import framework.mybatis.domain.NewDau;
import framework.mybatis.mapper.NewDauMapper;
import org.springframework.stereotype.Service;

@Service
public class NewDauService extends AbstractServiceImpl<NewDauMapper, NewDau> {

    public NewDau queryById(Integer id) {
        return getById(id);
    }

    public NewDau queryByUidDate(Integer uid, String date) {
        return lambdaQuery().eq(NewDau::getUid,uid).eq(NewDau::getDate,date).one();
    }
}
