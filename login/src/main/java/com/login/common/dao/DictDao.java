package com.login.common.dao;

import com.login.common.domain.DictDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 字典表
 * 
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-03 15:45:42
 */
@Mapper
public interface DictDao {

	@OnSqlDo(DataSourceType.LOGIN)
	DictDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<DictDO> list(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(DictDO dict);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(DictDO dict);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	List<DictDO> listType();
}
