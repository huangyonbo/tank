package com.login.bms.dao;

import com.login.bms.domain.RegisterMgrDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-10 18:02:45
 */
@Mapper
public interface RegisterMgrDao {

	@OnSqlDo(DataSourceType.BMS)
	RegisterMgrDO get(Integer id);
	@OnSqlDo(DataSourceType.BMS)
	List<RegisterMgrDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.BMS)
	int save(RegisterMgrDO registerMgr);
	@OnSqlDo(DataSourceType.BMS)
	int update(RegisterMgrDO registerMgr);
	@OnSqlDo(DataSourceType.BMS)
	int remove(Integer id);
}
