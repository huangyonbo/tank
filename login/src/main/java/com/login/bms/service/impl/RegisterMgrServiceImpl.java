package com.login.bms.service.impl;

import com.login.bms.dao.RegisterMgrDao;
import com.login.bms.domain.RegisterMgrDO;
import com.login.bms.service.RegisterMgrService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Data
public class RegisterMgrServiceImpl implements RegisterMgrService {
	@Autowired
	private RegisterMgrDao registerMgrDao;
	@Override
	public RegisterMgrDO get(Integer id){
		return registerMgrDao.get(id);
	};

	@Override
	public List<RegisterMgrDO> list(Map<String, Object> map){
		return registerMgrDao.list(map);
	}

	@Override
	public int save(RegisterMgrDO registerMgr){
		return registerMgrDao.save(registerMgr);
	}

	@Override
	public int update(RegisterMgrDO registerMgr){
		return registerMgrDao.update(registerMgr);
	}

	@Override
	public int remove(Integer id){
		return registerMgrDao.remove(id);
	}

	@Override
	public RegisterMgrDO getConfigByChannelId(int channelId) {
		List<RegisterMgrDO> datas = list(new HashMap<String,Object>(){
			{
				put("channelId",channelId);
			}
		});
		return datas != null && datas.size() > 0 ? datas.get(0) : null;
	}
}
