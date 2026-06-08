package framework.mybatis.service.impl;

import framework.mybatis.domain.ReqMember;
import framework.mybatis.mapper.ReqMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReqMemberService extends AbstractServiceImpl<ReqMemberMapper, ReqMember> {

    public ReqMember queryById(Integer id) {
        return getById(id);
    }

    public List<ReqMember> loadAll() {
        return list();
    }

    public int addReqMember(ReqMember reqMember) {
        save(reqMember);
        return reqMember.getId();
    }

    public boolean updateReqMember(ReqMember reqMember) {
        return updateById(reqMember);
    }

    public boolean deleteAllReqMember(Integer uid){
        return lambdaUpdate().eq(ReqMember::getUid,uid).remove();
    }

    public boolean deleteGuildAllReqMember(Integer guildId){
        return lambdaUpdate().eq(ReqMember::getReqGuildId,guildId).remove();
    }

    public boolean deleteOneReqMember(Integer guildId, Integer uid){
        return lambdaUpdate().eq(ReqMember::getReqGuildId,guildId).eq(ReqMember::getUid,uid).remove();
    }
}
