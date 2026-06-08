package com.login.common.dao;

import com.login.common.domain.LogDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 系统日志
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 15:45:42
 */
@Mapper
public interface LogDao {
	@OnSqlDo(DataSourceType.LOGIN)
	LogDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<LogDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(LogDO log);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(LogDO log);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
}
