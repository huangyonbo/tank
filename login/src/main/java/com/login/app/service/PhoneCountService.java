package com.login.app.service;

import com.login.app.domain.PhoneCountDO;

import java.util.List;
import java.util.Map;

/**
 * 手机号注册限制
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-09 12:11:41
 */
public interface PhoneCountService {
	PhoneCountDO get(String phone);
	List<PhoneCountDO> list(Map<String, Object> map);
	int count(Map<String, Object> map);
	int save(PhoneCountDO phoneCount);
	int update(PhoneCountDO phoneCount);
	int remove(String phone);
	int batchRemove(String[] phones);
}
