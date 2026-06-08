package com.login.app.controller;

import com.alibaba.fastjson.JSONObject;
import com.login.app.domain.*;
import com.login.app.service.*;
import com.login.bms.domain.NoticeDO;
import com.login.bms.service.DeviceInfoService;
import com.login.bms.service.GamePlayerService;
import com.login.bms.service.NoticeService;
import com.login.bms.service.RegisterMgrService;
import com.login.common.annotation.LimitCheck;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.service.GeneratorService;
import com.login.common.utils.R;
import com.login.common.utils.StringUtils;
import com.login.func.IFuncLogic;
import com.login.func.impl.FuncLogic;
import com.login.http.HttpClientApi;
import com.login.ip.service.LimitIpService;
import com.login.prop.domain.RealNameProperties;
import com.login.prop.domain.SmsProperties;
import com.login.prop.service.PropConfigService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    IFuncLogic funcLogic;

    @Autowired
    private RedisManager redisManager;

    @Autowired
    private LoginInfoService loginInfoService;

    @Autowired
    private PropConfigService propConfigService;

    //	@Autowired
    private AccountCancelRecordService accountCancelRecordService;

    @Autowired
    private RegisterMgrService registerMgrService;

    @Autowired
    private RegisterDeviceInfoService registerDeviceInfoService;

    @Autowired
    private SmsService smsService;
    @Autowired
    private GamePlayerService gamePlayerService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private HttpClientApi httpClientApi;

    @Autowired
    private PhoneCountService phoneCountService;

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private RolesService rolesService;

    @Autowired
    private NoticeService noticeService;

    @PostConstruct
    public void init() {
        redisManager.init();
        funcLogic = new FuncLogic();
        funcLogic.init();
    }

    @PostMapping("/reloadFunc")
    @ApiOperation("重新加载通用逻辑")
    public String reload() {
        funcLogic = new FuncLogic();
        if (funcLogic.init()) {
            return "SUCC";
        }
        return "FAIL";
    }

    @PostMapping("/login")
    @ApiOperation("app登录")
    @LimitCheck
    public Object accountLogin(LoginParamsDTO loginParams, HttpServletRequest request) {
        return funcLogic.execute(1, loginParams, loginInfoService, accountCancelRecordService, propConfigService, redisManager, smsService, gamePlayerService, rolesService);
    }

    @ApiOperation("登录二次校验")
    @PostMapping("/login_Auth")
    public Object loginAuth(@RequestBody LoginAuthParamsDTO tokenInfo) {
        return funcLogic.execute(2, tokenInfo, loginInfoService, propConfigService, redisManager);
    }

    @ApiOperation("快速登录")
    @PostMapping("/quickLogin")
    @LimitCheck
    public Object quickLogin(LoginParamsDTO loginParams, HttpServletRequest request) {
        return funcLogic.execute(3, loginParams, loginInfoService, propConfigService, registerMgrService, registerDeviceInfoService, redisManager);
    }

    @ApiOperation("获取后台该渠道的注册配置")
    @PostMapping("/getRegisterSet")
    public Object getRegisterSet(Integer channelId) {
        return funcLogic.execute(4, channelId, propConfigService, registerMgrService);
    }

    @ApiOperation("个性账号注册")
    @PostMapping("/accountRegister")
    public Object accountRegister(AccountRegisterDTO registerParams) {
        return funcLogic.execute(5, registerParams, propConfigService, registerMgrService, loginInfoService, registerDeviceInfoService, phoneCountService);
    }

    @ApiOperation("客户端获取随机账号和密码")
    @PostMapping("/getRandomUsernameAndPwd")
    public Object getRandomUsernameAndPwd() {
        int maxId = loginInfoService.getMaxId() + 1;
        String tmp = maxId + "a1"; // a1 -> 保证是字母数字组合
        if (tmp.length() < 12) {
            tmp += StringUtils.getRandomString(12 - tmp.length());
        }
        return R.result(0).put("username", tmp).put("password", StringUtils.getRandomString(10) + "a1");
    }

    @ApiOperation("手机号注册")
    @PostMapping("/phoneRegByAuthCode")
    public Object phoneRegByAuthCode(PhoneRegisterDTO registerParams) {
        return funcLogic.execute(6, registerParams, loginInfoService, smsService, propConfigService, registerDeviceInfoService, registerMgrService, phoneCountService);
    }

    @ApiOperation("绑定手机号")
    @PostMapping("/bindPhone")
    public Object bindPhoneWithType(@RequestBody BindPhoneDTO data) {
        return funcLogic.execute(7, data, loginInfoService, smsService, propConfigService, phoneCountService);
    }

    @ApiOperation("游客绑定手机号")
    @PostMapping("/touristBindPhone")
    public Object touristBindPhone(@RequestBody TouristBindPhoneDTO data) {
        return funcLogic.execute(8, data, loginInfoService, smsService, propConfigService, phoneCountService);
    }

    @ApiOperation("解绑手机")
    @PostMapping("/unBindPhone")
    public Object unBindPhone(@RequestBody UnBindPhoneDTO data) {
        return funcLogic.execute(9, data, loginInfoService, smsService, propConfigService, phoneCountService);
    }

    @ApiOperation("修改密码")
    @PostMapping("/changePassword")
    public Object changePassword(@RequestBody ChangePasswordDTO data) {
        return funcLogic.execute(10, data, loginInfoService, smsService, propConfigService);
    }

    @ApiOperation("恢复账号")
    @PostMapping("/resumeDelAccount")
    public Object resumeDelAccount(Integer uid) {
        return funcLogic.execute(11, uid, accountCancelRecordService);
    }

    @ApiOperation("后台重置手机号")
    @PostMapping("/phoneReset")
    public Object phoneReset(@RequestBody AdminOpDTO data) {
        return funcLogic.execute(12, data, loginInfoService);
    }

    @Autowired
    private LimitIpService limitIpService;

    @ApiOperation("后台重置密码")
    @PostMapping("/playerPasswordReset")
    public Object playerPasswordReset(@RequestBody AdminOpDTO data, HttpServletRequest request) {
        return funcLogic.execute(13, data, loginInfoService, limitIpService, request.getRemoteHost());
    }

    @ApiOperation("后台查找账号")
    @PostMapping("/findAccount")
    public Object findAccount(@RequestBody AdminOpDTO data) {
        return funcLogic.execute(14, data, loginInfoService);
    }

    @ApiOperation("客户端埋点上报设备号")
    @PostMapping("/reportDeviceInfo")
    public Object reportDeviceInfo(ReportDeviceDTO data) {
        return funcLogic.execute(15, data, deviceInfoService);
    }

    @ApiOperation("获取账号名")
    @PostMapping("/getUsername")
    public Object getUsername(@RequestBody AdminOpDTO data) {
        String username = data.getUsername();
        LoginInfoDO loginInfoEntity = null;
        if (username != null && username.length() > 0) {
            loginInfoEntity = loginInfoService.search(username);
        }
        if (loginInfoEntity == null) {
            int uid = data.getUid();
            loginInfoEntity = loginInfoService.get(uid);
        }
        if (loginInfoEntity == null) {
            return "未找到该玩家";
        }
        return "编号:" + loginInfoEntity.getId() + ",账号:" + loginInfoEntity.getUserName();
    }

    @ApiOperation("忘记密码功能")
    @PostMapping("/forgetPassword")
    public Object forgetPassword(ForgetPasswordDTO data) {
        return funcLogic.execute(16, data, loginInfoService, smsService, propConfigService);
    }

    @ApiOperation("实名认证")
    @PostMapping("/realNameAuth")
    public Object realNameAuth(@RequestBody RealNameAuthDTO data) {
        RealNameProperties realNameProperties = propConfigService.getProp("realName");
        return funcLogic.execute(17, data, realNameProperties, loginInfoService, httpClientApi);
    }

    @ApiOperation("游戏数据上报")
    @PostMapping("/gameReport")
    public Object gameReport(@RequestBody GameReportDTO data) {
        return R.ok();//这个功能没什么用
        //RealNameProperties realNameProperties = propConfigService.getProp("realName");
        //return funcLogic.execute(18,data,realNameProperties,loginInfoService,httpClientApi);
    }

    @ApiOperation("预警短信")
    @PostMapping("/warningMsg")
    public Object sendWarningMsg(@RequestBody WarningMsgDTO data) {
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        return funcLogic.execute(19, data, smsProperties, smsService);
    }

    @ApiOperation("获取短信验证码")
    @PostMapping("/smsService")
    public Object smsServiceNew(String phone, String userName, Integer codeType) {
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        return funcLogic.execute(20, phone, userName, codeType, smsProperties, loginInfoService, smsService);
    }

    @ApiOperation("后台封禁/解封账号")
    @PostMapping("/backFrozen")
    public Object backFrozen(Integer type, Integer uid) {
        return funcLogic.execute(21, type, uid, loginInfoService);
    }

    @ApiOperation("清理手机号注册限制")
    @PostMapping("/clearPhoneCount")
    public Object clearPhoneCount(String phone) {
        return funcLogic.execute(22, phone, phoneCountService);
    }

    @ApiOperation("清理设备号注册限制")
    @PostMapping("/clearRegisterCount")
    public Object clearRegisterCount(String deviceId) {
        return funcLogic.execute(23, deviceId, registerDeviceInfoService);
    }

    @ApiOperation("修改用户渠道")
    @PostMapping("/changeChannelByUid")
    public Object changeChannelByUid(Integer uid, Integer oldChannel, Integer newChannel) {
        return funcLogic.execute(24, uid, oldChannel, newChannel, loginInfoService, rolesService);
    }

    @ApiOperation("通过Uid获取注册设备号")
    @GetMapping("/getRegisterIdByUid")
    public Object getRegisterIdByUid(Integer uid) {
        return funcLogic.execute(25, uid, loginInfoService);
    }


    @ApiOperation("通过Uid修改用户注册设备号")
    @PostMapping("/changeRegisterIdByUid")
    public Object changeRegisterIdByUid(Integer uid, String regId) {
        return funcLogic.execute(26, uid, regId, loginInfoService, rolesService);
    }

    @ApiOperation("获取公告")
    @PostMapping("/getNotice")
    public Object getNotic(Integer type, String channel) {
        Map<String, Object> maps = new HashMap<>();
//		maps.put("id",id);
//		maps.put("channel",channel);
        maps.put("type", type);
        log.info("{} {} {}  {}", type, channel, maps);
        long time = new Date().getTime();
        List<NoticeDO> list = noticeService.list(maps).stream().filter(a ->
                a.getEndTime().getTime() > time && a.getStart().getTime() < time
        ).filter(a -> a.getChannel().contains(channel) || a.getChannel().contains("-1")).collect(Collectors.toList());
        log.info("{}", JSONObject.toJSONString(list));
        return JSONObject.toJSONString(list);
    }
}
