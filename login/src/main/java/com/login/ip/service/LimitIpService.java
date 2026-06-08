package com.login.ip.service;

import com.login.ip.domain.LimitIpDO;

import java.util.List;
import java.util.Map;

/**
 * ip禁用
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 11:30:55
 */
public interface LimitIpService {
	
	LimitIpDO get(Integer id);

	List<LimitIpDO> list(Map<String, Object> map);

	int count(Map<String, Object> map);
	int save(LimitIpDO limitIp);
	
	int update(LimitIpDO limitIp);
	
	int remove(Integer id);

	int batchRemove(Integer[] ids);
}
