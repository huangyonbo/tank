package com.login.app.controller;

import com.login.app.service.LoginInfoService;
import com.login.app.service.RegisterDeviceInfoService;
import com.login.bms.service.RegisterMgrService;
import com.login.common.annotation.LimitCheck;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.service.GeneratorService;
import com.login.common.utils.R;
import com.login.func.FuncUtils;
import com.login.func.IChannel;
import com.login.http.HttpClientApi;
import com.login.prop.service.PropConfigService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/app")
@Slf4j
public class ChannelController {

    @Autowired
    private LoginInfoService loginInfoService;

    @Autowired
    private PropConfigService propConfigService;

    @Autowired
    private HttpClientApi httpClientApi;

    @Autowired
    private RedisManager redisManager;

    @Autowired
    private RegisterMgrService registerMgrService;

    @Autowired
    private RegisterDeviceInfoService registerDeviceInfoService;

    @Autowired
    private GeneratorService generatorService;

    Map<String, IChannel> channels = new HashMap<>();

    @PostConstruct
    public void init(){
        FuncUtils.loadChannels(channels);
        channels.values().forEach(channel -> {
            channel.init(0,httpClientApi,propConfigService,
                    loginInfoService,redisManager,
                    registerMgrService,registerDeviceInfoService);
        });
    }

    @PostMapping("/reload/{channel}")
    @ApiOperation("重新加载渠道算法")
    public String reload(@PathVariable("channel") String channel){
//        IChannel iChannel = FuncUtils.load("config/Func.jar",channel);
//        if (iChannel != null){
//            iChannel.init(1,httpClientApi,propConfigService,
//                    loginInfoService,redisManager,
//                    registerMgrService,registerDeviceInfoService);
//            channels.put(iChannel.name(),iChannel);
//            log.info("reload {} success",channel);
//            return "success";
//        }
//        log.error("reload {} fail",channel);
        return "fail";
    }

    @PostMapping("/{channel}")
    @ApiOperation("渠道登录")
    @LimitCheck
    public R doLogin(@RequestBody String reqParams, HttpServletRequest request,@PathVariable("channel") String channel){
        IChannel iChannel = channels.get(channel.toLowerCase());
        if (iChannel != null){
            return iChannel.login(reqParams);
        }
        log.error("找不到{}配置",channel);
        return R.result(-1).put("error","找不到渠道配置");
    }
}
