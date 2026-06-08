package framework.mybatis.service.impl;

import framework.mybatis.domain.*;
import framework.mybatis.mapper.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ActivityService extends AbstractServiceImpl<ActivityMapper, Activity> {

    public List<Activity> loadActives(){
        return lambdaQuery().eq(Activity::getStatus, 1).eq(Activity::getSend, 1).list();
    }

    public List<Activity> searchByIds(String ids){
        return lambdaQuery().in(Activity::getId, Arrays.asList(ids.split(","))).list();
    }
}
