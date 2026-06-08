package com.login.app.service.impl;

import com.login.app.dao.RegisterDeviceInfoDao;
import com.login.app.domain.RegisterDeviceInfoDO;
import com.login.app.service.RegisterDeviceInfoService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Data
public class RegisterDeviceInfoServiceImpl implements RegisterDeviceInfoService {
	@Autowired
	private RegisterDeviceInfoDao registerDeviceInfoDao;
	@Override
	public RegisterDeviceInfoDO get(Integer id){
		return registerDeviceInfoDao.get(id);
	};

	@Override
	public List<RegisterDeviceInfoDO> list(Map<String, Object> map){
		return registerDeviceInfoDao.list(map);
	}

	@Override
	public int count(Map<String, Object> map){
		return registerDeviceInfoDao.count(map);
	}

	@Override
	public int save(RegisterDeviceInfoDO registerDeviceInfo){
		return registerDeviceInfoDao.save(registerDeviceInfo);
	}

	@Override
	public int update(RegisterDeviceInfoDO registerDeviceInfo){
		return registerDeviceInfoDao.update(registerDeviceInfo);
	}

	@Override
	public int remove(Integer id){
		return registerDeviceInfoDao.remove(id);
	}

	@Override
	public int batchRemove(int[] ids){
		return registerDeviceInfoDao.batchRemove(ids);
	}
}
