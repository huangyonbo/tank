package com.login.bms.service;

import com.login.bms.domain.RegisterMgrDO;

import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-05-10 18:02:45
 */
public interface RegisterMgrService {
	
	RegisterMgrDO get(Integer id);

	List<RegisterMgrDO> list(Map<String, Object> map);

	int save(RegisterMgrDO registerMgr);
	
	int update(RegisterMgrDO registerMgr);
	
	int remove(Integer id);

	RegisterMgrDO getConfigByChannelId(int channelId);
}
