package framework.mybatis.service.impl;

import framework.mybatis.domain.SeakingConfig;
import framework.mybatis.mapper.SeakingConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeakingConfigService extends AbstractServiceImpl<SeakingConfigMapper, SeakingConfig> {

    public List<SeakingConfig> loadAll() {
        return list();
    }
}
