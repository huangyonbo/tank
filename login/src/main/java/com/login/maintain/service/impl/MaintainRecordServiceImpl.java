package com.login.maintain.service.impl;

import com.login.maintain.dao.MaintainRecordDao;
import com.login.maintain.domain.MaintainRecordDO;
import com.login.maintain.service.MaintainRecordService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Data
public class MaintainRecordServiceImpl implements MaintainRecordService {

	@Autowired
	private MaintainRecordDao maintainRecordDao;

	@Override
	public MaintainRecordDO get(Integer id){
		return maintainRecordDao.get(id);
	};

	@Override
	public List<MaintainRecordDO> list(){
		return maintainRecordDao.list();
	}
}
