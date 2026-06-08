package com.login.ip.service.impl;

import com.login.ip.dao.LimitIpDao;
import com.login.ip.domain.LimitIpDO;
import com.login.ip.service.LimitIpService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Data
public class LimitIpServiceImpl implements LimitIpService {
	@Autowired
	private LimitIpDao limitIpDao;
	@Override
	public LimitIpDO get(Integer id){
		return limitIpDao.get(id);
	};

	@Override
	public List<LimitIpDO> list(Map<String, Object> map){
		return limitIpDao.list(map == null ? new HashMap<>() : map);
	}

	@Override
	public int count(Map<String, Object> map){
		return limitIpDao.count(map);
	}

	@Override
	public int save(LimitIpDO limitIp){
		return limitIpDao.save(limitIp);
	}

	@Override
	public int update(LimitIpDO limitIp){
		return limitIpDao.update(limitIp);
	}

	@Override
	public int remove(Integer id){
		return limitIpDao.remove(id);
	}

	@Override
	public int batchRemove(Integer[] ids){
		return limitIpDao.batchRemove(ids);
	}
}
