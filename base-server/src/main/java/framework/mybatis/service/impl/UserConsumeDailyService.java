package framework.mybatis.service.impl;

import framework.mybatis.domain.UserConsumeDaily;
import framework.mybatis.mapper.UserConsumeDailyMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserConsumeDailyService extends AbstractServiceImpl<UserConsumeDailyMapper, UserConsumeDaily> {

    public UserConsumeDaily searchById(int uid, String date) {
        return lambdaQuery().eq(UserConsumeDaily::getUserId, uid).eq(UserConsumeDaily::getBizDate, date).one();
    }

    public List<UserConsumeDaily> searchByIds(List<Integer> uids, String date) {
        return lambdaQuery().in(UserConsumeDaily::getUserId, uids).eq(UserConsumeDaily::getBizDate, date).list();
    }
}
