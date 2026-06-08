package com.login.prop.dao;


import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.prop.domain.PropConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 系统配置
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 18:04:02
 */
@Mapper
public interface PropConfigDao {

	@OnSqlDo(DataSourceType.LOGIN)
	PropConfigDO get(String id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<PropConfigDO> list(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String,Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(PropConfigDO propConfig);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(PropConfigDO propConfig);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(String id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(String[] ids);
}
