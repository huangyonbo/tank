package com.login.app.dao;

import com.login.app.domain.AccountCancelRecordDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-12 17:23:56
 */
//@Mapper
public interface AccountCancelRecordDao {
	@OnSqlDo(DataSourceType.GAME)
	AccountCancelRecordDO get(Long id);
	@OnSqlDo(DataSourceType.GAME)
	List<AccountCancelRecordDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.GAME)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.GAME)
	int save(AccountCancelRecordDO accountCancelRecord);
	@OnSqlDo(DataSourceType.GAME)
	int update(AccountCancelRecordDO accountCancelRecord);
	@OnSqlDo(DataSourceType.GAME)
	int remove(Long id);
}
