package com.login.app.service.impl;

import com.login.app.dao.LoginInfoDao;
import com.login.app.domain.LoginInfoDO;
import com.login.app.service.LoginInfoService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Data
public class LoginInfoServiceImpl implements LoginInfoService {

	@Autowired
	private LoginInfoDao loginInfoDao;

	@Override
	public LoginInfoDO get(Integer id){
		return loginInfoDao.get(id);
	};

	@Override
	public int save(LoginInfoDO loginInfo){
		if (loginInfo.getSex() == null){
			loginInfo.setSex(0);
		}
		if (loginInfo.getStatus() == null){
			loginInfo.setStatus(0);
		}
		return loginInfoDao.save(loginInfo);
	}
	
	@Override
	public int update(LoginInfoDO loginInfo){
		return loginInfoDao.update(loginInfo);
	}
	
	@Override
	public int remove(Integer id){
		return loginInfoDao.remove(id);
	}

	@Override
	public LoginInfoDO search(String account) {
		List<LoginInfoDO> list = loginInfoDao.search(account);
		if (list != null && list.size() > 0){
			return list.get(0);
		}
		return null;
	}

	@Override
	public int getMaxId() {
		List<Map<String,Object>> maps = loginInfoDao.getMaxId("select max(ID) id from login_info");
		if (maps != null && maps.size() > 0){
			Map<String,Object> map = maps.get(0);
			return map == null ? 0 : Integer.parseInt(map.get("id").toString());
		}
		return 0;
	}

	@Override
	public void updatePassword(LoginInfoDO loginInfo) {
		loginInfoDao.updatePassword(loginInfo);
	}
}
