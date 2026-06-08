package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.RoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 角色与菜单对应关系
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 11:08:59
 */
@Mapper
public interface RoleMenuDao {
	@OnSqlDo(DataSourceType.LOGIN)
	RoleMenuDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<RoleMenuDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(RoleMenuDO roleMenu);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(RoleMenuDO roleMenu);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	List<Long> listMenuIdByRoleId(Long roleId);
	@OnSqlDo(DataSourceType.LOGIN)
	int removeByRoleId(Long roleId);
	@OnSqlDo(DataSourceType.LOGIN)
	int removeByMenuId(Long menuId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchSave(List<RoleMenuDO> list);
}
