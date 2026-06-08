package framework.mybatis.service.impl;

import framework.mybatis.domain.GuildRepoRecord;
import framework.mybatis.mapper.GuildRepoRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildRepoRecordService extends AbstractServiceImpl<GuildRepoRecordMapper, GuildRepoRecord> {

    public int Add(GuildRepoRecord guildRepoRecord){
        save(guildRepoRecord);
        return 1;
    }

    public List<GuildRepoRecord> loadAll(Integer uid, Integer guildId) {
        return lambdaQuery().
                eq(GuildRepoRecord::getGuildId, guildId).
                and(w -> w.eq(GuildRepoRecord::getUid, uid).or().eq(GuildRepoRecord::getTakeUid, uid)).
                orderByDesc(GuildRepoRecord::getId).
                last(" limit 0, 50").list();
    }
}
