package framework.mybatis.service.impl;

import framework.mybatis.domain.LotteryRecorder;
import framework.mybatis.mapper.LotteryRecorderMapper;
import org.springframework.stereotype.Service;

@Service
public class LotteryRecorderService extends AbstractServiceImpl<LotteryRecorderMapper, LotteryRecorder> {

    public boolean insert(LotteryRecorder recorder) {
        return save(recorder);
    }
}
