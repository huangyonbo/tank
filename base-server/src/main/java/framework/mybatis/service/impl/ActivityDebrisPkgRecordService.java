package framework.mybatis.service.impl;

import framework.mybatis.domain.*;
import framework.mybatis.mapper.*;
import org.springframework.stereotype.Service;


@Service
public class ActivityDebrisPkgRecordService extends AbstractServiceImpl<ActivityDebrisPkgRecordMapper, ActivityDebrisPkgRecord> {

    public Long addOne(ActivityDebrisPkgRecord activityDebrisPkgRecord){
        if (save(activityDebrisPkgRecord)){
            return activityDebrisPkgRecord.getId();
        }
        return 0L;
    }
}
