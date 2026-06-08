package framework.mybatis.service.impl;

import framework.mybatis.data.SimpleRole;
import framework.mybatis.domain.Roles;
import framework.mybatis.mapper.RolesMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolesService extends AbstractServiceImpl<RolesMapper, Roles> {

    public Roles queryById(Integer id) {
        return getById(id);
    }

    public List<Roles> queryByIds(List<Integer> ids) {
        return lambdaQuery().in(Roles::getId, ids).list();
    }

    public boolean resetOnline(){
        return lambdaUpdate().set(Roles::getOnline,0).update();
    }

    public List<SimpleRole> loadSimpleRole(){
        return lambdaQuery().
                select(Roles::getId, Roles::getUserName, Roles::getHeadId,Roles::getProxyId).
                list().stream().map(role -> {
            SimpleRole simpleRole = new SimpleRole();
            simpleRole.setHeadId(role.getHeadId());
            simpleRole.setId(role.getId());
            simpleRole.setProxyId(role.getProxyId());
            simpleRole.setUserName(role.getUserName());
            return simpleRole;
        }).collect(Collectors.toList());
    }

    public List<String> loadNames() {
        return lambdaQuery().select(Roles::getUserName).list().stream().map(Roles::getUserName).collect(Collectors.toList());
    }
    public List<Integer> loadAllRoles(){
        return lambdaQuery().select(Roles::getId).gt(Roles::getBombItem, 0).list().stream().map(Roles::getId).collect(Collectors.toList());
    }
    public boolean UpdateBomCoinCountAndBomCoin(Integer roleId, long bom_coin, long bomItemCount){
        return lambdaUpdate()
                .eq(Roles::getId, roleId)
                .set(Roles::getBombItem, bomItemCount)
                .set(Roles::getBombcoin, bom_coin)
                .update();
    }

    public boolean UpdateInviteViP(Integer roleId, int vip){
        return lambdaUpdate()
                .eq(Roles::getId, roleId)
                .set(Roles::getVip, vip)
                .update();
    }
}
