package com.login.app.dao;

import com.login.app.domain.RolesDO;
import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 游戏用户
 * @author keyking
 * @email keyking@163.com
 * @date 2021-11-23 11:10:57
 */
@Mapper
public interface RolesDao {
	@OnSqlDo(DataSourceType.GAME)
	void updateChannel(@Param("uid") Integer uid, @Param("old")Integer old,@Param("target")Integer target);
	@OnSqlDo(DataSourceType.GAME)
	void updateRegisterDeviceId(@Param("uid") Integer uid,@Param("regId")String regId);
    @OnSqlDo(DataSourceType.GAME)
    RolesDO search(Integer id);
}
