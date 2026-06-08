package com.login.bms.dao;

import com.login.bms.domain.NoticeDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Mapper
public interface NoticeDao {

    @OnSqlDo(DataSourceType.BMS)
    List<NoticeDO> list(Map<String,Object> map);
}
