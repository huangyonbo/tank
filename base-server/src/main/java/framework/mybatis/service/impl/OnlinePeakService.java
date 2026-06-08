package framework.mybatis.service.impl;

import framework.mybatis.domain.OnlinePeak;
import framework.mybatis.mapper.OnlinePeakMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnlinePeakService extends AbstractServiceImpl<OnlinePeakMapper, OnlinePeak> {

    public OnlinePeak queryById(Integer id) {
        return getById(id);
    }

    public OnlinePeak queryByChanelDate(Integer channel, String date) {
        List<OnlinePeak> res =  lambdaQuery().eq(OnlinePeak::getChannel,channel).eq(OnlinePeak::getDate,date).list();
        if (res == null || res.size() <= 0) {
            return null;
        }
        return res.get(0);
    }
}
