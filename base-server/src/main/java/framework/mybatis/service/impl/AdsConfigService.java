package framework.mybatis.service.impl;

import framework.mybatis.domain.AdsConfig;
import framework.mybatis.mapper.AdsConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdsConfigService extends AbstractServiceImpl<AdsConfigMapper, AdsConfig> {

    public List<AdsConfig> loadAll(){
        return lambdaQuery().list();
    }
}
