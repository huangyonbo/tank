package framework.mybatis.service.impl;

import framework.mybatis.domain.PayChannel;
import framework.mybatis.mapper.PayChannelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayChannelService extends AbstractServiceImpl<PayChannelMapper, PayChannel> {

    public List<PayChannel> loadAll(){
        return super.list();
    }

    public boolean updateCurScore(Integer channel,Long value){
        return lambdaUpdate().set(PayChannel::getCurPubScore,value).eq(PayChannel::getId,channel).update();
    }

    public boolean updateMaxScore(Integer channel,Long value){
        return lambdaUpdate().set(PayChannel::getMaxPubScore,value).eq(PayChannel::getId,channel).update();
    }
}
