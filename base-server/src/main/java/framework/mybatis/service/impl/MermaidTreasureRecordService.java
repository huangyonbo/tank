package framework.mybatis.service.impl;

import framework.mybatis.domain.MermaidTreasureRecord;
import framework.mybatis.mapper.MermaidTreasureRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MermaidTreasureRecordService extends AbstractServiceImpl<MermaidTreasureRecordMapper, MermaidTreasureRecord> {

    public void addOne(MermaidTreasureRecord mermaidTreasureRecord) {
        save(mermaidTreasureRecord);
    }
    public void addBach(List mermaidTreasureRecord) {
        saveBatch(mermaidTreasureRecord);
    }

    public List<MermaidTreasureRecord> loadAll() {
        return lambdaQuery().orderByDesc(MermaidTreasureRecord::getId).last(" limit 0, 15").list();
    }
}
