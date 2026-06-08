package com.login.bms.dao;

import com.login.bms.domain.DeviceInfoDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-17 10:17:44
 */
@Mapper
public interface DeviceInfoDao {

	@OnSqlDo(DataSourceType.BMS)
	DeviceInfoDO get(Integer id);
	@OnSqlDo(DataSourceType.BMS)
	List<DeviceInfoDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.BMS)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.BMS)
	int save(DeviceInfoDO deviceInfo);
	@OnSqlDo(DataSourceType.BMS)
	int update(DeviceInfoDO deviceInfo);
	@OnSqlDo(DataSourceType.BMS)
	int remove(Integer id);
	@OnSqlDo(DataSourceType.BMS)
	int batchRemove(Integer[] ids);
}
