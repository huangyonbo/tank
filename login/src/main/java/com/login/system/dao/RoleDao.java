package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.RoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 角色
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-02 20:24:47
 */
@Mapper
public interface RoleDao {
	@OnSqlDo(DataSourceType.LOGIN)
	RoleDO get(Long roleId);
	@OnSqlDo(DataSourceType.LOGIN)
	List<RoleDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(RoleDO role);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(RoleDO role);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long roleId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] roleIds);
}
