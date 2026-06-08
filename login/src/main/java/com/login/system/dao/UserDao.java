package com.login.system.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.system.domain.UserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 09:45:11
 */
@Mapper
public interface UserDao {
	@OnSqlDo(DataSourceType.LOGIN)
	UserDO get(Long userId);
	@OnSqlDo(DataSourceType.LOGIN)
	List<UserDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(UserDO user);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(UserDO user);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long userId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] userIds);
	@OnSqlDo(DataSourceType.LOGIN)
	Long[] listAllDept();

}
