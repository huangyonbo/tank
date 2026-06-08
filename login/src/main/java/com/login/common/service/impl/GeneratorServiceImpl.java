package com.login.common.service.impl;

import com.login.common.dao.GeneratorMapper;
import com.login.common.service.GeneratorService;
import com.login.common.utils.GenUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;


@Service
public class GeneratorServiceImpl implements GeneratorService {

	@Autowired
    GeneratorMapper generatorMapper;
	
	@Override
	public List<Map<String, Object>> list() {
		List<Map<String, Object>> list = generatorMapper.list();
		return list;
	}

	@Override
	public byte[] generatorCode(String[] tableNames) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		for (String tableName : tableNames){
			//查询表信息
			Map<String, String> table = generatorMapper.get(tableName);
			//查询列信息
			List<Map<String, String>> columns = generatorMapper.listColumns(tableName);
			//生成代码
			GenUtils.generatorCode(table, columns, zip);
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}
	
	@Override
	public int insert(String sql) {
		return generatorMapper.insert(sql);
	}

	@Override
	public int update(String sql) {
		return generatorMapper.update(sql);
	}

	@Override
	public List<Map<String, String>> getByName(String tableName){
		return generatorMapper.listColumns(tableName);
	}

	@Override
	public List<Map<String, Object>> selectMoreDataBySql(String sql){
		return generatorMapper.selectMoreDataBySql(sql);
	}
}
