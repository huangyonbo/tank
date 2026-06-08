package com.login.app.service.impl;

import com.login.app.dao.PhoneCountDao;
import com.login.app.domain.PhoneCountDO;
import com.login.app.service.PhoneCountService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Data
public class PhoneCountServiceImpl implements PhoneCountService {
	@Autowired
	private PhoneCountDao phoneCountDao;
	@Override
	public PhoneCountDO get(String phone){
		return phoneCountDao.get(phone);
	};

	@Override
	public List<PhoneCountDO> list(Map<String, Object> map){
		return phoneCountDao.list(map);
	}

	@Override
	public int count(Map<String, Object> map){
		return phoneCountDao.count(map);
	}

	@Override
	public int save(PhoneCountDO phoneCount){
		return phoneCountDao.save(phoneCount);
	}

	@Override
	public int update(PhoneCountDO phoneCount){
		return phoneCountDao.update(phoneCount);
	}

	@Override
	public int remove(String phone){
		return phoneCountDao.remove(phone);
	}

	@Override
	public int batchRemove(String[] phones){
		return phoneCountDao.batchRemove(phones);
	}
}
