package framework.mybatis.service.impl;

import framework.mybatis.domain.ActivityLuckyPuzzleRecord;
import framework.mybatis.mapper.ActivityLuckyPuzzleRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class ActivityLuckyPuzzleRecordService extends AbstractServiceImpl<ActivityLuckyPuzzleRecordMapper, ActivityLuckyPuzzleRecord> {

    public int addOne(ActivityLuckyPuzzleRecord activityLuckyPuzzleRecord){
        if (save(activityLuckyPuzzleRecord)){
            return activityLuckyPuzzleRecord.getId();
        }
        return 0;
    }
}
