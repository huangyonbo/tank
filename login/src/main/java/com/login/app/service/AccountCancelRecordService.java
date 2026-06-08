package com.login.app.service;

import com.login.app.domain.AccountCancelRecordDO;

import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-12 17:23:56
 */
public interface AccountCancelRecordService {
	
	AccountCancelRecordDO get(Long id);

	AccountCancelRecordDO getByUid(Integer id);

	List<AccountCancelRecordDO> list(Map<String, Object> map);

	int count(Map<String, Object> map);

	int save(AccountCancelRecordDO accountCancelRecord);
	
	int update(AccountCancelRecordDO accountCancelRecord);
	
	int remove(Long id);
}
