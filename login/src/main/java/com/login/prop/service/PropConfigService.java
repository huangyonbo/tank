package com.login.prop.service;

import com.login.prop.domain.PropConfigDO;

import java.util.List;
import java.util.Map;

/**
 * 系统配置
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-08 18:04:02
 */
public interface PropConfigService {
	
	PropConfigDO get(String id);

	List<PropConfigDO> list(Map<String, Object> map);

	int count(Map<String, Object> map);

	int save(PropConfigDO propConfig);
	
	int update(PropConfigDO propConfig);
	
	int remove(String id);

	int batchRemove(String[] ids);

	<T> T getProp(String id);
}
