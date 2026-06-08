package framework.mybatis.service.impl;

import framework.mybatis.domain.GuildMember;
import framework.mybatis.mapper.GuildMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildMemberService extends AbstractServiceImpl<GuildMemberMapper, GuildMember> {

    public GuildMember queryById(Integer id) {
        return getById(id);
    }

    public List<GuildMember> loadAll() {
        return list();
    }

    public boolean updateMember(GuildMember guildMember){
        return updateById(guildMember);
    }

    public boolean addMember(GuildMember guildMember){
        return save(guildMember);
    }

    public boolean deleteMember(Integer uid){
        return removeById(uid);
    }

    public boolean deleteAllMember(Integer guildId){
        return lambdaUpdate().eq(GuildMember::getGuildId,guildId).remove();
    }
}
