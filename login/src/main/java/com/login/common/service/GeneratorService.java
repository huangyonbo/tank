/**
 * 
 */
package com.login.common.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author 1992lcg@163.com
 * @Time 2017年9月6日
 * @description
 * 
 */
@Service
public interface GeneratorService {
	List<Map<String, Object>> list();
	byte[] generatorCode(String[] tableNames);
	int insert(String sql);
	int update(String sql);
	List<Map<String, String>> getByName(String tableName);
	List<Map<String,Object>> selectMoreDataBySql(String sqlStr);
}
