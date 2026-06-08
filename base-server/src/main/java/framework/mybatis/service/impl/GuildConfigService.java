package framework.mybatis.service.impl;

import framework.mybatis.domain.GuildConfig;
import framework.mybatis.mapper.GuildConfigMapper;
import org.springframework.stereotype.Service;

//@Service
public class GuildConfigService extends AbstractServiceImpl<GuildConfigMapper, GuildConfig> {

    public GuildConfig queryById() {
        return getById(1);
    }
}
