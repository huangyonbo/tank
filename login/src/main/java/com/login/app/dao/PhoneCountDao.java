package com.login.app.dao;

import com.login.app.domain.PhoneCountDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 手机号注册限制
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-09 12:11:41
 */
@Mapper
public interface PhoneCountDao {

	@OnSqlDo(DataSourceType.LOGIN)
	PhoneCountDO get(String phone);
	@OnSqlDo(DataSourceType.LOGIN)
	List<PhoneCountDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(PhoneCountDO phoneCount);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(PhoneCountDO phoneCount);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(String phone);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(String[] phones);
}
