package framework.mybatis.service.impl;

import framework.mybatis.domain.RunRecord;
import framework.mybatis.mapper.RunRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class RunRecordService extends AbstractServiceImpl<RunRecordMapper, RunRecord> {

    public boolean addOne(RunRecord rec){
        return save(rec);
    }

    public RunRecord queryById(Integer id) {
        return getById(id);
    }
}
