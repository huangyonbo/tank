package framework.mybatis.service.impl;

import framework.mybatis.domain.PlayerWeaponState;
import framework.mybatis.mapper.PlayerWeaponStateMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PlayerWeaponStateService extends AbstractServiceImpl<PlayerWeaponStateMapper, PlayerWeaponState> {

    public PlayerWeaponState queryByUid(Integer uid) {
        if (uid == null || uid <= 0) {
            return null;
        }
        return lambdaQuery().eq(PlayerWeaponState::getUid, uid).one();
    }

    public boolean upsertByUid(PlayerWeaponState state) {
        if (state == null || state.getUid() == null || state.getUid() <= 0) {
            return false;
        }
        PlayerWeaponState old = queryByUid(state.getUid());
        if (old == null) {
            return save(state);
        }
        return lambdaUpdate()
                .eq(PlayerWeaponState::getUid, state.getUid())
                .set(PlayerWeaponState::getWeaponsLevels, state.getWeaponsLevels())
                .set(PlayerWeaponState::getTankSpeedLevel, state.getTankSpeedLevel())
                .set(PlayerWeaponState::getTankArmorLevel, state.getTankArmorLevel())
                .update();
    }
}
