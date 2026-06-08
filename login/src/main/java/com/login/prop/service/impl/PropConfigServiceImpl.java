package com.login.prop.service.impl;

import com.login.prop.dao.PropConfigDao;
import com.login.prop.domain.PropConfigDO;
import com.login.prop.service.PropConfigService;
import com.login.prop.util.PropUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Data
public class PropConfigServiceImpl implements PropConfigService {
	@Autowired
	private PropConfigDao propConfigDao;
	@Override
	public PropConfigDO get(String id){
		return propConfigDao.get(id);
	};

	@Override
	public List<PropConfigDO> list(Map<String, Object> map){
		return propConfigDao.list(map);
	}

	@Override
	public int count(Map<String, Object> map){
		return propConfigDao.count(map);
	}

	@Override
	public int save(PropConfigDO propConfig){
		return propConfigDao.save(propConfig);
	}

	@Override
	public int update(PropConfigDO propConfig){
		return propConfigDao.update(propConfig);
	}

	@Override
	public int remove(String id){
		return propConfigDao.remove(id);
	}

	@Override
	public int batchRemove(String[] ids){
		return propConfigDao.batchRemove(ids);
	}

	@Override
	public <T> T getProp(String id) {
		PropConfigDO propConfigDO = get(id);
		return PropUtils.transform(propConfigDO);
	}
}
