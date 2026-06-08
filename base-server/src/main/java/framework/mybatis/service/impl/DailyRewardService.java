package framework.mybatis.service.impl;

import framework.mybatis.domain.DailyReward;
import framework.mybatis.mapper.DailyRewardMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DailyRewardService extends AbstractServiceImpl<DailyRewardMapper, DailyReward> {

    public List<DailyReward> loadAll() {
        return lambdaQuery().eq(DailyReward::getStatus,1).list();
    }

    public List<DailyReward> query(String ids) {
        return lambdaQuery().eq(DailyReward::getStatus, 1).in(DailyReward::getId, Arrays.asList(ids.split(","))).list();
    }
}
