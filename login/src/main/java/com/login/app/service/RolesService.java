package com.login.app.service;

import com.login.app.domain.RolesDO;

/**
 * 游戏用户
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-11-23 11:10:57
 */
public interface RolesService {
	boolean updateChannel(Integer uid,Integer old,Integer target);

	boolean updateRegisterDeviceId(Integer uid,String regId);

    RolesDO search(Integer id);
}
