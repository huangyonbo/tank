package com.login.oa.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.oa.domain.NotifyDO;
import com.login.oa.domain.NotifyDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 通知通告
 * 
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-05 17:11:16
 */
@Mapper
public interface NotifyDao {
	@OnSqlDo(DataSourceType.LOGIN)
	NotifyDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<NotifyDO> list(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(NotifyDO notify);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(NotifyDO notify);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	List<NotifyDO> listByIds(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	int countDTO(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	List<NotifyDTO> listDTO(Map<String, Object> map);
}
