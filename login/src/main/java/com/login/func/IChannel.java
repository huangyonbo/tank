package com.login.func;

import com.login.app.service.LoginInfoService;
import com.login.app.service.RegisterDeviceInfoService;
import com.login.bms.service.RegisterMgrService;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.utils.R;
import com.login.http.HttpClientApi;
import com.login.prop.service.PropConfigService;

public interface IChannel {

    void init(int type,HttpClientApi httpClientApi, PropConfigService propConfigService,
              LoginInfoService loginInfoService, RedisManager redisManager,
              RegisterMgrService registerMgrService,
              RegisterDeviceInfoService registerDeviceInfoService);

    String name();

    R login(String reqParams);
}
