package com.login.bms.dao;

import com.login.bms.domain.GamePlayerEntity;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GamePlayerDao {
    @OnSqlDo(DataSourceType.GAME)
    GamePlayerEntity getByID(Integer id);
}
