package framework.mybatis.service.impl;

import framework.mybatis.domain.OfflineData;
import framework.mybatis.mapper.OfflineDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OfflineDataService extends AbstractServiceImpl<OfflineDataMapper, OfflineData> {

    public OfflineData queryById(Integer id) {
        return getById(id);
    }

    public List<OfflineData> queryByUid(Integer uid){
        return lambdaQuery().eq(OfflineData::getUid,uid).eq(OfflineData::getState,0).list();
    }

    public void setState(Integer uid){
        lambdaUpdate().eq(OfflineData::getUid,uid).set(OfflineData::getState,1).update();
    }

    public void setState(List<Integer> ids){
        lambdaUpdate().in(OfflineData::getId,ids).set(OfflineData::getState,1).update();
    }

    @Transactional
    public boolean saveBatch(List<OfflineData> entityList) {
        return saveBatch(entityList, DEFAULT_BATCH_SIZE);
    }
}
