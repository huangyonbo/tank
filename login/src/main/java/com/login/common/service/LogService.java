package com.login.common.service;

import com.login.common.domain.LogDO;
import com.login.common.domain.PageDO;
import com.login.common.utils.Query;
import org.springframework.stereotype.Service;
@Service
public interface LogService {
	void save(LogDO logDO);
	PageDO<LogDO> queryList(Query query);
	int remove(Long id);
	int batchRemove(Long[] ids);
}
