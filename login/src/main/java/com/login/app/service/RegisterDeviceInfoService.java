package com.login.app.service;

import com.login.app.domain.RegisterDeviceInfoDO;

import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-11 08:12:04
 */
public interface RegisterDeviceInfoService {
	
	RegisterDeviceInfoDO get(Integer id);

	List<RegisterDeviceInfoDO> list(Map<String, Object> map);

	int count(Map<String, Object> map);

	int save(RegisterDeviceInfoDO registerDeviceInfo);
	
	int update(RegisterDeviceInfoDO registerDeviceInfo);
	
	int remove(Integer id);

	int batchRemove(int[] ids);
}
