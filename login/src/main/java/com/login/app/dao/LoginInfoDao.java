package com.login.app.dao;

import com.login.app.domain.LoginInfoDO;
import com.login.common.dao.GeneratorSql;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;
import java.util.Map;


/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 17:01:33
 */
@Mapper
public interface LoginInfoDao {
	@OnSqlDo(DataSourceType.LOGIN)
	LoginInfoDO get(Integer id);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(LoginInfoDO loginInfo);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(LoginInfoDO loginInfo);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Integer ID);
	@OnSqlDo(DataSourceType.LOGIN)
    List<LoginInfoDO> search(String account);
	@SelectProvider(type= GeneratorSql.class,method="excuteSql")
	@OnSqlDo(DataSourceType.LOGIN)
	List<Map<String,Object>> getMaxId(String sqlStr);
	@OnSqlDo(DataSourceType.LOGIN)
	void updatePassword(LoginInfoDO loginInfo);
}
