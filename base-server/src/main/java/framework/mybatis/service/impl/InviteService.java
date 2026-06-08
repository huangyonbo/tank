package framework.mybatis.service.impl;

import framework.mybatis.domain.Invite;
import framework.mybatis.mapper.InviteMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InviteService extends AbstractServiceImpl<InviteMapper, Invite> {

    public List<Invite> searchById(int id) {
        return lambdaQuery().eq(Invite::getInviterId, id).list();
    }

    public List<Invite> searchByIds(List<Integer> ids) {
        return lambdaQuery().in(Invite::getInviterId, ids).list();
    }

    public List<Invite> searchAll() {
        return lambdaQuery().list();
    }

    public int searchBingId(int id) {
        Invite one = lambdaQuery().eq(Invite::getReceiverId, id).one();
        if (one == null) {
            return 0;
        }
        return one.getInviterId();
    }
}
