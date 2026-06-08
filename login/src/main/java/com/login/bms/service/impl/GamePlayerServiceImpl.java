package com.login.bms.service.impl;

import com.login.bms.dao.GamePlayerDao;
import com.login.bms.domain.GamePlayerEntity;
import com.login.bms.service.GamePlayerService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Data
public class GamePlayerServiceImpl implements GamePlayerService {
    @Autowired
    GamePlayerDao gamePlayerDao;


    @Override
    public GamePlayerEntity getById(Integer id) {
        return gamePlayerDao.getByID(id);
    }
}
