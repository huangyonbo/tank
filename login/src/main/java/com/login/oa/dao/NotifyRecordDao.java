package com.login.oa.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import com.login.oa.domain.NotifyRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 通知通告发送记录
 * 
 * @author chglee
 * @email 1992lcg@163.com
 * @date 2017-10-09 17:18:45
 */
@Mapper
public interface NotifyRecordDao {
	@OnSqlDo(DataSourceType.LOGIN)
	NotifyRecordDO get(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	List<NotifyRecordDO> list(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int save(NotifyRecordDO notifyRecord);
	@OnSqlDo(DataSourceType.LOGIN)
	int update(NotifyRecordDO notifyRecord);
	@OnSqlDo(DataSourceType.LOGIN)
	int remove(Long id);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemove(Long[] ids);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchSave(List<NotifyRecordDO> records);
	@OnSqlDo(DataSourceType.LOGIN)
	Long[] listNotifyIds(Map<String, Object> map);
	@OnSqlDo(DataSourceType.LOGIN)
	int removeByNotifbyId(Long notifyId);
	@OnSqlDo(DataSourceType.LOGIN)
	int batchRemoveByNotifbyId(Long[] notifyIds);
	@OnSqlDo(DataSourceType.LOGIN)
	int changeRead(NotifyRecordDO notifyRecord);
}
