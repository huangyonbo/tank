package com.login.app.service.impl;

import com.login.app.dao.AccountCancelRecordDao;
import com.login.app.domain.AccountCancelRecordDO;
import com.login.app.service.AccountCancelRecordService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


//@Service
@Data
public class AccountCancelRecordServiceImpl implements AccountCancelRecordService {

	@Autowired
	private AccountCancelRecordDao accountCancelRecordDao;

	@Override
	public AccountCancelRecordDO get(Long id){
		return accountCancelRecordDao.get(id);
	};

	@Override
	public  AccountCancelRecordDO getByUid(Integer uid){
		List<AccountCancelRecordDO> datas = list(new HashMap<String,Object>(){{
			put("uid",uid);
		}});
		if (datas != null && datas.size() > 0){
			return datas.get(0);
		}
		return null;
	}

	@Override
	public List<AccountCancelRecordDO> list(Map<String, Object> map){
		return accountCancelRecordDao.list(map);
	}

	@Override
	public int count(Map<String, Object> map){
		return accountCancelRecordDao.count(map);
	}

	@Override
	public int save(AccountCancelRecordDO accountCancelRecord){
		return accountCancelRecordDao.save(accountCancelRecord);
	}

	@Override
	public int update(AccountCancelRecordDO accountCancelRecord){
		return accountCancelRecordDao.update(accountCancelRecord);
	}

	@Override
	public int remove(Long id){
		return accountCancelRecordDao.remove(id);
	}

}
