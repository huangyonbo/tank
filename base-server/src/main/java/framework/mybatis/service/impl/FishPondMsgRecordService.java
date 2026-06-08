package framework.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import framework.mybatis.domain.FishPondMsgRecord;
import framework.mybatis.mapper.FishPondMsgRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FishPondMsgRecordService extends AbstractServiceImpl<FishPondMsgRecordMapper, FishPondMsgRecord> {

    public List<Map<String, Object>> queryByUid(Integer uid) {
        return listMaps(new QueryWrapper<FishPondMsgRecord>().lambda().
                select(FishPondMsgRecord::getType,
                        FishPondMsgRecord::getCreateTime,
                        FishPondMsgRecord::getNickName,
                        FishPondMsgRecord::getCost)
                .eq(FishPondMsgRecord::getUid, uid)
                .orderByDesc(FishPondMsgRecord::getCreateTime)
                .last(" limit 0, 200"));
    }

    public void deleteAll() {
       lambdaUpdate().remove();
    }

}
