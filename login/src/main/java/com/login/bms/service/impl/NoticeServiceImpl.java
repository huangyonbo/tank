package com.login.bms.service.impl;

import com.login.bms.dao.NoticeDao;
import com.login.bms.domain.NoticeDO;
import com.login.bms.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
public class NoticeServiceImpl implements NoticeService {
    @Autowired
    private NoticeDao noticeDao;
    @Override
    public List<NoticeDO> list(Map<String, Object> map) {
        return noticeDao.list(map);
    }
}
