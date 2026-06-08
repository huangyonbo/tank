package com.login.common.dao;

import com.login.common.mybatis.DataSourceType;
import com.login.common.mybatis.ano.OnSqlDo;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface GeneratorMapper {
	@Select("select table_name tableName, engine, table_comment tableComment, create_time createTime from information_schema.tables"
			+ " where table_schema = (select database())")
	@OnSqlDo(DataSourceType.LOGIN)
	List<Map<String, Object>> list();

	@Select("select count(*) from information_schema.tables where table_schema = (select database())")
	@OnSqlDo(DataSourceType.LOGIN)
	int count(Map<String, Object> map);

	@Select("select table_name tableName, engine, table_comment tableComment, create_time createTime from information_schema.tables \r\n"
			+ "	where table_schema = (select database()) and table_name = #{tableName}")
	@OnSqlDo(DataSourceType.LOGIN)
	Map<String, String> get(String tableName);

	@Select("select column_name columnName, data_type dataType, column_comment columnComment, column_key columnKey, extra from information_schema.columns where table_name = #{tableName} and table_schema = (select database()) order by ordinal_position")
	@OnSqlDo(DataSourceType.LOGIN)
	List<Map<String, String>> listColumns(String tableName);
	
	@SelectProvider(type=GeneratorSql.class,method="excuteSql")
	@OnSqlDo(DataSourceType.LOGIN)
	List<Map<String,Object>> selectMoreDataBySql(String sqlStr);
	
	@UpdateProvider(type=GeneratorSql.class,method="excuteSql")
	@OnSqlDo(DataSourceType.LOGIN)
	int update(String excuteSqlStr);
	
	@InsertProvider(type=GeneratorSql.class,method="excuteSql")
	@OnSqlDo(DataSourceType.LOGIN)
	int insert(String excuteSqlStr);
}
