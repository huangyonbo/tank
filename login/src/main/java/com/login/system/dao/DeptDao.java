package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.DeptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 部门管理
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 15:35:39
 */
@Mapper
public interface DeptDao {
	@OnSqlDo(DataSourceType.LOGIN)
	DeptDO get(Long deptId);
	@OnSqlDo(DataSourceType.LOGIN)
	List<DeptDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(DeptDO dept);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(DeptDO dept);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long deptId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] deptIds);
	@OnSqlDo(DataSourceType.LOGIN)
	Long[] listParentDept();
	@OnSqlDo(DataSourceType.LOGIN)
	int getDeptUserNumber(Long deptId);
}
