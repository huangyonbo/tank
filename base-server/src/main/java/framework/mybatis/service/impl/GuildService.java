package framework.mybatis.service.impl;

import framework.mybatis.domain.Guild;
import framework.mybatis.mapper.GuildMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildService extends AbstractServiceImpl<GuildMapper, Guild> {

    public Guild queryById(Integer id) {
        return getById(id);

    }

    public List<Guild> loadAll() {
        return lambdaQuery().ne(Guild::getGuildStatus,2).list();
    }

    public boolean updateGuild(Guild guild){
        return updateById(guild);
    }

    public boolean updatePowerCfg(Integer id, String cfg) {
        return lambdaUpdate().set(Guild::getPowerConfig, cfg).eq(Guild::getId, id).update();
    }

    public boolean updateRepoCapacity(Integer id, Integer count) {
        return lambdaUpdate().set(Guild::getRepoCapacity, count).eq(Guild::getId, id).update();
    }

    public boolean delete(Integer id){
        return removeById(id);
    }
}
