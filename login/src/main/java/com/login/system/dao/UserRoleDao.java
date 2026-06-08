package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 用户与角色对应关系
 * 
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 11:08:59
 */
@Mapper
public interface UserRoleDao {
	@OnSqlDo(DataSourceType.LOGIN)
	UserRoleDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<UserRoleDO> list(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(UserRoleDO userRole);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(UserRoleDO userRole);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	List<Long> listRoleId(Long userId);
	@OnSqlDo(DataSourceType.LOGIN)
	int removeByUserId(Long userId);
	@OnSqlDo(DataSourceType.LOGIN)
	int removeByRoleId(Long roleId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchSave(List<UserRoleDO> list);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemoveByUserId(Long[] ids);
}
