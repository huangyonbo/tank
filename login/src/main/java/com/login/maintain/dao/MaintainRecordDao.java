package com.login.maintain.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.maintain.domain.MaintainRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 维护设置
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 09:45:02
 */
@Mapper
public interface MaintainRecordDao {
	@OnSqlDo(DataSourceType.BMS)
	MaintainRecordDO get(Integer id);
	@OnSqlDo(DataSourceType.BMS)
	List<MaintainRecordDO> list();
}
