package framework.mybatis.service.impl;

import framework.mybatis.domain.WarningItemScore;
import framework.mybatis.mapper.WarningItemScoreMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarningItemScoreService extends AbstractServiceImpl<WarningItemScoreMapper, WarningItemScore> {

    public WarningItemScore queryById(Integer id) {
        return getById(id);
    }

    public WarningItemScore queryByUid(Integer uid) {
        List<WarningItemScore> res = lambdaQuery().eq(WarningItemScore::getUid,uid).list();
        if (res.size() <= 0) {
            return null;
        }
        return res.get(0);
    }
}
