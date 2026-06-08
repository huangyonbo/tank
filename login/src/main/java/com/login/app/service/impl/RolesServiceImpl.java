package com.login.app.service.impl;

import com.login.app.dao.RolesDao;
import com.login.app.domain.RolesDO;
import com.login.app.service.RolesService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Data
public class RolesServiceImpl implements RolesService {

	@Autowired
	private RolesDao rolesDao;

	@Override
	public boolean updateChannel(Integer uid, Integer old, Integer target) {
		try {
			rolesDao.updateChannel(uid,old,target);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean updateRegisterDeviceId(Integer uid,String regId) {
		try {
			rolesDao.updateRegisterDeviceId(uid,regId);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

    @Override
    public RolesDO search(Integer id) {
        try {
            return rolesDao.search(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
