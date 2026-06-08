package framework.mybatis.service.impl;

import framework.mybatis.domain.SendItems;
import framework.mybatis.mapper.SendItemsMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.util.List;

@Service
public class SendItemsService extends AbstractServiceImpl<SendItemsMapper, SendItems> {

    public void updateState(String id,Integer state){
        SendItems mail = getById(id);
        if (mail == null) {
            return;
        }
        if (state == 1 && mail.getState() != 0){
            return;
        }
        if (state == 2 && mail.getState() != 1){  //删除邮件
            return;
        }
        mail.setState(state);
        updateById(mail);
    }

    public void updateState(List<String> ids, Integer state, String format){
        lambdaUpdate()
                .set(SendItems::getUpdateTime,format)
                .set(SendItems::getState, state).in(SendItems::getId, ids).update();
    }

    public List<SendItems> queryByConds(Integer uid) {
        return lambdaQuery().
                eq(SendItems::getState,0).
                nested(wrapper -> wrapper.eq(SendItems::getRecUid, uid)).list();
    }

    public void updateVipAndProp(String id,String senderHas, String recHas){
        lambdaUpdate().set(SendItems::getSenderHas, senderHas).
                set(SendItems::getRecHas, recHas).
                eq(SendItems::getId,id).
                update();
    }

    public  List<SendItems> getList(Integer uid){
        List<SendItems> list = lambdaQuery().eq(SendItems::getRecUid, uid)
                .eq(SendItems::getState, 0).list();
        return list;
    }
}
