package com.login.bms.service;

import com.login.bms.domain.DeviceInfoDO;

import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-17 10:17:44
 */
public interface DeviceInfoService {
	
	DeviceInfoDO get(Integer id);

	List<DeviceInfoDO> list(Map<String, Object> map);

	int count(Map<String, Object> map);

	int save(DeviceInfoDO deviceInfo);
	
	int update(DeviceInfoDO deviceInfo);
	
	int remove(Integer id);

	int batchRemove(Integer[] ids);
}
