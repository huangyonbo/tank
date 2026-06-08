package com.login.app.dao;

import com.login.app.domain.RegisterDeviceInfoDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-11 08:12:04
 */
@Mapper
public interface RegisterDeviceInfoDao {

	@OnSqlDo(DataSourceType.LOGIN)
	RegisterDeviceInfoDO get(Integer id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<RegisterDeviceInfoDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(RegisterDeviceInfoDO registerDeviceInfo);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(RegisterDeviceInfoDO registerDeviceInfo);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Integer id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(int[] ids);
}
