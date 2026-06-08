package framework.mybatis.service.impl;

import framework.mybatis.domain.ArenaRecord;
import framework.mybatis.mapper.ArenaRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class ArenaRecordService extends AbstractServiceImpl<ArenaRecordMapper, ArenaRecord> {

    public int addOne(ArenaRecord rec){
        if (save(rec)){
            return rec.getId();
        }
        return 0;
    }
}
