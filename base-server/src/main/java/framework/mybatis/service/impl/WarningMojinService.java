package framework.mybatis.service.impl;

import framework.mybatis.domain.WarningMojin;
import framework.mybatis.mapper.WarningMojinMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarningMojinService extends AbstractServiceImpl<WarningMojinMapper, WarningMojin> {

    public WarningMojin queryById(Integer id) {
        return getById(id);
    }

    public WarningMojin queryByUid(Integer uid) {
        List<WarningMojin> res = lambdaQuery().eq(WarningMojin::getUid,uid).list();
        if (res.size() <= 0) {
            return null;
        }
        return res.get(0);
    }
}
