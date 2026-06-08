package framework.mybatis.service.impl;

import framework.mybatis.domain.Mails;
import framework.mybatis.mapper.MailsMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.util.List;

@Service
public class MailsService extends AbstractServiceImpl<MailsMapper, Mails> {

    public void updateState(String id,Integer state){
        Mails mail = getById(id);
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

    public void updateState(List<String> ids, Integer state){
        lambdaUpdate().set(Mails::getState, state).in(Mails::getId, ids).update();
    }

    public List<Mails> queryByConds(DateFormat format,String mailId, Integer uid, Integer channel) {
        String nowTime = format.format(System.currentTimeMillis());
        boolean condition = mailId != null && mailId.contains("-");
        if (condition){
            mailId = mailId.replace("-","");
            mailId = mailId.replace(" ","");
            mailId = mailId.replace(":","");
        }
        return lambdaQuery().
                eq(Mails::getState,0).
                gt(StringUtils.isNotEmpty(mailId), Mails::getId, mailId).
                nested(wrapper -> wrapper.eq(Mails::getRecUid, uid).or().eq(Mails::getRecUid, -1)).
                nested(wrapper -> wrapper.eq(Mails::getChannel, channel).or().eq(Mails::getChannel, -1)).
                nested(wrapper -> wrapper.gt(Mails::getEndTime, nowTime).or().eq(Mails::getEndTime, "")).
                list();
    }

    public void updateVipAndProp(String id, Integer senderVip, Integer recVip, String senderHas, String recHas){
        lambdaUpdate().set(Mails::getSenderHas, senderHas).
                set(Mails::getRecHas, recHas).
                set(Mails::getSenderVip, senderVip).
                set(Mails::getRecVip, recVip).
                eq(Mails::getId,id).
                update();
    }
}
