package com.login.bms.service;

import com.login.bms.domain.NoticeDO;

import java.util.List;
import java.util.Map;

public interface NoticeService {
    List<NoticeDO> list(Map<String, Object> map);
}
