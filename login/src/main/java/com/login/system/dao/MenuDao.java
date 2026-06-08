package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 菜单管理
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 09:45:09
 */
@Mapper
public interface MenuDao {
	@OnSqlDo(DataSourceType.LOGIN)
	MenuDO get(Long menuId);
	@OnSqlDo(DataSourceType.LOGIN)
	List<MenuDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(MenuDO menu);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(MenuDO menu);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long menuId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(long[] menuIds);
	@OnSqlDo(DataSourceType.LOGIN)
	List<MenuDO> listMenuByUserId(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<String> listUserPerms(Long id);
}
