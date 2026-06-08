package framework.mybatis.service.impl;

import framework.mybatis.domain.GuildRepository;
import framework.mybatis.mapper.GuildRepositoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildRepositoryService extends AbstractServiceImpl<GuildRepositoryMapper, GuildRepository> {

    public GuildRepository queryById(Integer id) {
        return getById(id);
    }

    public List<GuildRepository> loadAll() {
        return list();
    }

    public boolean add(GuildRepository guildRepository){
        return save(guildRepository);
    }

    public boolean delete(String recordId){
        return lambdaUpdate().eq(GuildRepository::getRecordId,recordId).remove();
    }

    public boolean removeAll(Integer guildId){
        return lambdaUpdate().eq(GuildRepository::getGuildId,guildId).remove();
    }

    public boolean removeByUid(Integer uid) {
        return lambdaUpdate().eq(GuildRepository::getUid, uid).remove();
    }

}
