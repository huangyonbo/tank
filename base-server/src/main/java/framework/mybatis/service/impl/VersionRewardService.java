package framework.mybatis.service.impl;

import framework.mybatis.domain.VersionReward;
import framework.mybatis.mapper.VersionRewardMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VersionRewardService extends AbstractServiceImpl<VersionRewardMapper, VersionReward> {

    public List<VersionReward> loadAll() {
        return lambdaQuery().eq(VersionReward::getStatus,1).list();
    }

    public List<VersionReward> query(List<Integer> ids) {
        return lambdaQuery().eq(VersionReward::getStatus,1).in(VersionReward::getId,ids).list();
    }
}
