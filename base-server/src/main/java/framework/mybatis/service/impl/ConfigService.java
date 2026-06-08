package framework.mybatis.service.impl;

import framework.mybatis.domain.Config;
import framework.mybatis.mapper.ConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class ConfigService extends AbstractServiceImpl<ConfigMapper, Config> {

    public Config queryById(String id) {
        return getById(id);
    }

}
