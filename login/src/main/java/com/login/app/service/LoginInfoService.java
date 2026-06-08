package com.login.app.service;

import com.login.app.domain.LoginInfoDO;

/**
 * 
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 17:01:33
 */
public interface LoginInfoService {
	
	LoginInfoDO get(Integer id);
	
	int save(LoginInfoDO loginInfo);
	
	int update(LoginInfoDO loginInfo);
	
	int remove(Integer id);

	LoginInfoDO search(String account);

    int getMaxId();

    void updatePassword(LoginInfoDO loginInfo);
}
