package com.login.ip.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.ip.domain.LimitIpDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * ip禁用
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 11:30:55
 */
@Mapper
public interface LimitIpDao {
	@OnSqlDo(DataSourceType.LOGIN)
	LimitIpDO get(Integer id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<LimitIpDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(LimitIpDO limitIp);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(LimitIpDO limitIp);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Integer id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Integer[] ids);
}
