package com.login.bms.service.impl;

import com.login.bms.dao.DeviceInfoDao;
import com.login.bms.domain.DeviceInfoDO;
import com.login.bms.service.DeviceInfoService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Data
public class DeviceInfoServiceImpl implements DeviceInfoService {
	@Autowired
	private DeviceInfoDao deviceInfoDao;
	@Override
	public DeviceInfoDO get(Integer id){
		return deviceInfoDao.get(id);
	};

	@Override
	public List<DeviceInfoDO> list(Map<String, Object> map){
		return deviceInfoDao.list(map);
	}

	@Override
	public int count(Map<String, Object> map){
		return deviceInfoDao.count(map);
	}

	@Override
	public int save(DeviceInfoDO deviceInfo){
		return deviceInfoDao.save(deviceInfo);
	}

	@Override
	public int update(DeviceInfoDO deviceInfo){
		return deviceInfoDao.update(deviceInfo);
	}

	@Override
	public int remove(Integer id){
		return deviceInfoDao.remove(id);
	}

	@Override
	public int batchRemove(Integer[] ids){
		return deviceInfoDao.batchRemove(ids);
	}
}
