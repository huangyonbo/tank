package com.login.maintain.service;

import com.login.maintain.domain.MaintainRecordDO;

import java.util.List;

/**
 * 维护设置
 * 
 * @author keyking
 * @email keyking@163.com
 * @date 2021-10-13 09:45:02
 */
public interface MaintainRecordService {
	
	MaintainRecordDO get(Integer id);

	List<MaintainRecordDO> list();
}
