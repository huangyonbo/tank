package com.login.func.impl;

import com.alibaba.fastjson.JSONObject;
import com.login.app.domain.*;
import com.login.app.service.*;
import com.login.bms.domain.DeviceInfoDO;
import com.login.bms.domain.GamePlayerEntity;
import com.login.bms.domain.RegisterMgrDO;
import com.login.bms.service.DeviceInfoService;
import com.login.bms.service.GamePlayerService;
import com.login.bms.service.RegisterMgrService;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.utils.*;
import com.login.enums.*;
import com.login.func.AbstractFuncLogic;
import com.login.func.FuncUtils;
import com.login.http.HttpClientApi;
import com.login.ip.domain.LimitIpDO;
import com.login.ip.service.LimitIpService;
import com.login.prop.domain.AppProperties;
import com.login.prop.domain.RealNameProperties;
import com.login.prop.domain.SmsProperties;
import com.login.prop.service.PropConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
public class FuncLogic extends AbstractFuncLogic {

    private final String DEFAULT_PASSWORD = "L1r5LXNXnalBT83caZg/hV2C9tG6/dA1cfyoMhVeNUQ=";

    private final String DEFAULT_SALT = "7nLrRgpojt";

    private Map<String, ErrorInfo> errorInfos = new HashMap<>();

    @Override
    public boolean init() {
        register(1, "accountLogin");
        register(2, "loginAuth");
        register(3, "quickLogin");
        register(4, "getRegisterSet");
        register(5, "accountRegister");
        register(6, "phoneRegByAuthCode");
        register(7, "bindPhoneWithType");
        register(8, "touristBindPhone");
        register(9, "unBindPhone");
        register(10, "changePassword");
        register(11, "resumeDelAccount");
        register(12, "phoneReset");
        register(13, "playerPasswordReset");
        register(14, "findAccount");
        register(15, "reportDeviceInfo");
        register(16, "forgetPassword");
        register(17, "realNameAuth");
        register(18, "gameReport");
        register(19, "sendWarningMsg");
        register(20, "sendVerifyMsg");
        register(21, "backFrozen");
        register(22, "clearPhoneCount");
        register(23, "clearRegisterCount");
        register(24, "changeChannelByUid");
        register(25, "getRegisterIdByUid");
        register(26, "changeRegisterIdByUid");
        return true;
    }


    R accountLogin(Map<Integer, Object> params) {
        LoginParamsDTO loginParam = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        AccountCancelRecordService accountCancelRecordService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        RedisManager redisManager = getParams(4, params);
        SmsService smsService = getParams(5, params);
        GamePlayerService gamePlayerService = getParams(6, params);
        RolesService rolesService = getParams(7, params);
        String username = loginParam.getUserName();
        ErrorInfo errorInfo = errorInfos.get(username);
        if (errorInfo != null && errorInfo.getCount() >= 5) {
            long left = (errorInfo.getTime() - System.currentTimeMillis()) / 1000;
            if (left > 0) {
                String m = "", s = "";
                if (left > 60) {
                    int _m = (int) (left / 60);
                    m = _m + "分钟";
                    left = Math.max(0, left - _m * 60);
                }
                if (left >= 0) {
                    s = left + "秒";
                }
                String errorMsg = "您的账号已被锁定，请于" + m + s + "后重试";
                return R.result(LoginResultEnum.MIS_MATCH.ordinal()).put("errorMsg", errorMsg);
            }
        }
        LoginInfoDO loginInfoDo = loginInfoService.search(username);
        if (loginInfoDo == null) {
            return R.result(LoginResultEnum.NO_ACCOUNT.ordinal());
        }
        if (loginInfoDo.getStatus() != 0) {
            return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
        }
        RolesDO rolesDO = rolesService.search(loginInfoDo.getId());
        if (rolesDO != null && rolesDO.getStatus() != 0) {
            return R.result(LoginResultEnum.SYS_FROZEN.ordinal());
        }
//        AccountCancelRecordDO cancelDO = accountCancelRecordService.getByUid(loginInfoDo.getId());
//        if (cancelDO != null){
//            return R.result(LoginResultEnum.USER_CANCLE.ordinal());
//        }
        //比较登录渠道
        if (loginInfoDo.getChannelId() != loginParam.getSdkType()) {
            return R.result(LoginResultEnum.SDK_TYPE_ERROR.ordinal());
        }
        AppProperties appProperties = propConfigService.getProp("sysProp");
        GamePlayerEntity player = gamePlayerService.getById(loginInfoDo.getId());
        //获取盐值
        String salt = loginInfoDo.getSalt();
        if (StringUtils.isEmpty(salt)) {
            salt = appProperties.getSalt();
        }
        String expectPwd = CryptoUtils.getSHA256(loginParam.getPassword() + salt, "Base64");
        JSONObject info = new JSONObject();
        try {
            //校验密码
            if (!loginInfoDo.getPassword().equals(expectPwd)) {
                if (errorInfo == null) {
                    errorInfo = new ErrorInfo();
                    errorInfos.put(username, errorInfo);
                }
                errorInfo.setCount(errorInfo.getCount() + 1);
                if (errorInfo.getCount() >= 5) {
                    errorInfo.setTime(System.currentTimeMillis() + 3600000);
                }
                int leftCount = 5 - errorInfo.getCount();
                String errorMsg;
                if (leftCount <= 0) {
                    errorMsg = "密码错误，您的账号已被锁定，请于1小时后重试";
                } else {
                    errorMsg = "密码错误，您还剩余" + leftCount + "次输入机会，请输入正确的登录密码";
                }
                return R.result(LoginResultEnum.MIS_MATCH.ordinal()).put("errorMsg", errorMsg);
            }
//            info.put("手机验证",(loginInfoDo.getPhone()!=null&&!loginInfoDo.getPhone().isEmpty()));
//            info.put("设备验证",(player!=null&&player.getLoginDevice()!=null&&!player.getLoginDevice().isEmpty())&&!player.getLoginDevice().equals(loginParam.getDeviceId()));
//            if((player!=null&&player.getLoginDevice()!=null&&!player.getLoginDevice().isEmpty())&&!player.getLoginDevice().equals(loginParam.getDeviceId())&&loginInfoDo.getPhone()!=null&&!loginInfoDo.getPhone().isEmpty()){
//                if (loginParam.getVerify()==null||loginParam.getVerify().isEmpty()){
//                    return R.error(LoginResultEnum.DEVICE_ID_FAIL.ordinal()).put("errorMsg","登录设备号变更").put("phone",loginInfoDo.getPhone());
//                }
//                if (!loginInfoDo.getPhone().equals(loginParam.getPhone())){
//                    return R.error(LoginResultEnum.FAILED.ordinal()).put("errorMsg","请输入正确的绑定手机号");
//                }
//                SmsProperties smsProp = propConfigService.getProp("smsProp");
//                int result = smsProp.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.WARNING.ordinal(), loginInfoDo.getPhone() ,loginParam.getVerify());
//                if (result != RegisterResultEnum.SUCCESS.ordinal()){
//                    return R.error(LoginResultEnum.FAILED.ordinal()).put("errorMsg","验证码错误").put("phone",loginInfoDo.getPhone());
//                }
//            }
            //登录成功后清理错误信息
            errorInfos.remove(username);
            int uid = loginInfoDo.getId();
            loginInfoDo.setIp(loginParam.getRealIp());
            loginInfoService.update(loginInfoDo);
            //生成二次校验token
            String token = TokenUtils.generateToken(uid, redisManager);
            return R.ok().put("code", LoginResultEnum.SUCCESS.ordinal())
                    .put("result", LoginResultEnum.SUCCESS.ordinal())
                    .put("uid", uid)
                    .put("token", token);
        } finally {
            log.info(info.toString());
        }
    }

    AuthResultVO loginAuth(Map<Integer, Object> params) {
        LoginAuthParamsDTO loginAuthParam = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        PropConfigService propConfigService = getParams(2, params);
        RedisManager redisManager = getParams(3, params);
        AuthResultVO authResultVO = new AuthResultVO();
        if (loginAuthParam == null || StringUtils.isEmpty(loginAuthParam.getToken()) || loginAuthParam.getUid() <= 0) {
            authResultVO.setMessage("请求参数错误");
            return authResultVO;
        }
        int uid = loginAuthParam.getUid();
        if (!TokenUtils.verifyToken(uid, loginAuthParam.getToken(), redisManager)) {
            authResultVO.setMessage("token校验失败");
            return authResultVO;
        }
        LoginInfoDO loginInfoDO = loginInfoService.get(uid);
        if (loginInfoDO == null) {
            authResultVO.setMessage("获取账号信息失败");
            return authResultVO;
        }
        AppProperties appProperties = propConfigService.getProp("sysProp");
        authResultVO.setSex(loginInfoDO.getSex() == null ? 0 : loginInfoDO.getSex());
        authResultVO.setChannelId(loginInfoDO.getChannelId());
        authResultVO.setPhone(loginInfoDO.getPhone());
        authResultVO.setRecruiter(loginInfoDO.getRecruiter());
        authResultVO.setCode(LoginResultEnum.SUCCESS.ordinal());
        authResultVO.setExtend(loginAuthParam.getExtend());
        if (StringUtils.isEmpty(loginInfoDO.getNickname())) {
            //authResultVO.setName(appProperties.getNickName() + uid);
            authResultVO.setName(String.valueOf(uid));
        } else {
            authResultVO.setName(loginInfoDO.getNickname());
        }
        authResultVO.setIp(loginInfoDO.getIp());
        authResultVO.setHeadUrl(StringUtils.EMPTY);
        return authResultVO;
    }

    R quickLogin(Map<Integer, Object> params) {
        LoginParamsDTO loginParam = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        PropConfigService propConfigService = getParams(2, params);
        RegisterMgrService registerMgrService = getParams(3, params);
        RegisterDeviceInfoService registerDeviceInfoService = getParams(4, params);
        RedisManager redisManager = getParams(5, params);
        //根据账号获取玩家登录信息
        String username = loginParam.getUserName();
        String password;
        String deviceID = loginParam.getDeviceId() == null ? "" : loginParam.getDeviceId();
        int channelId = loginParam.getSdkType();
        AppProperties appProperties = propConfigService.getProp("sysProp");
        DeviceRegisterCount registerCount = null;
        if (appProperties.checkAccountRegister() == 1) {
            registerCount = FuncUtils.checkDeviceLimit(appProperties, registerMgrService, registerDeviceInfoService, channelId, deviceID);
            log.info("检查设备限制：username:{} channelId:{} deviceId:{} can:{} use:{}", username, channelId, deviceID, registerCount.getCanRegisterCount(), registerCount.getAlreadyRegisterCount());
            if (registerCount.getCanRegisterCount() <= 0) {
                return R.result(RegisterResultEnum.DEVICE_LIMIT.ordinal());
            }
        }
        LoginInfoDO loginInfoDO = loginInfoService.search(username);
        String salt = StringUtils.getRandomString(10);
        int isNew = 0;
        if (loginInfoDO == null) {
            //未注册
            isNew = 1;
            password = StringUtils.getRandomString(12);
            String hashPassword = CryptoUtils.getSHA256(password + salt, "Base64");
            Date now = new Date();
            loginInfoDO = LoginInfoDO.builder()
                    .userName(username)
                    .password(hashPassword)
                    .salt(salt)
                    .sex(0)
                    .phone(StringUtils.EMPTY)
                    .registerTime(now)
                    .loginTime(now)
                    .registerType(RegisterTypeEnum.QUICK.ordinal())
                    .channelId(channelId)
                    .registerDeviceId(deviceID)
                    .recruiter(StringUtils.EMPTY)
                    .pi(StringUtils.EMPTY)
                    .nickname(StringUtils.EMPTY)
                    .province(StringUtils.EMPTY)
                    .city(StringUtils.EMPTY)
                    .country(StringUtils.EMPTY)
                    .status(0)
                    .build();
            loginInfoService.save(loginInfoDO);
            if (registerCount != null) {
                //更新注册设备信息
                RegisterDeviceInfoDO registerDeviceInfoDO = registerCount.getRegisterDeviceInfoDO();
                if (registerDeviceInfoDO.getId() == 0) {
                    registerDeviceInfoDO.setAccountCount(1);
                    registerDeviceInfoService.save(registerDeviceInfoDO);
                } else {
                    registerDeviceInfoDO.setAccountCount(registerCount.getAlreadyRegisterCount() + 1);
                    registerDeviceInfoService.update(registerDeviceInfoDO);
                }
            }
        } else {
            password = loginInfoDO.getPassword();
            String hashPassword = CryptoUtils.getSHA256(password + salt, "Base64");
            //重置数据库的密码和对应盐值
            loginInfoDO.setPassword(hashPassword);
            loginInfoDO.setSalt(salt);
            loginInfoService.update(loginInfoDO);
        }
        String token = TokenUtils.generateToken(loginInfoDO.getId(), redisManager);
        return R.result(RegisterResultEnum.SUCCESS.ordinal()).put("token", token).put("data", password).put("isNew", isNew).put("uid", loginInfoDO.getId());
    }

    R getRegisterSet(Map<Integer, Object> params) {
        Integer channelId = getParams(0, params);
        PropConfigService propConfigService = getParams(1, params);
        RegisterMgrService registerMgrService = getParams(2, params);
        AppProperties appProperties = propConfigService.getProp("sysProp");
        int phoneRegister = appProperties.checkPhoneRegister();
        int accountRegister = appProperties.checkAccountRegister();
        int deviceRegisterLimit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
        RegisterMgrDO registerMgr = registerMgrService.getConfigByChannelId(channelId);
        RegisterSetVO registerSet = new RegisterSetVO(phoneRegister, accountRegister, deviceRegisterLimit);
        if (registerMgr != null) {
            registerSet.setPhone_register(registerMgr.getPhoneRegister());
            registerSet.setAccount_register(registerMgr.getAccountRegister());
            registerSet.setRegister_limit(registerMgr.getRegisterLimit());
        }
        return R.ok().put("data", registerSet);
    }

    R accountRegister(Map<Integer, Object> params) {
        AccountRegisterDTO accountRegister = getParams(0, params);
        PropConfigService propConfigService = getParams(1, params);
        RegisterMgrService registerMgrService = getParams(2, params);
        LoginInfoService loginInfoService = getParams(3, params);
        RegisterDeviceInfoService registerDeviceInfoService = getParams(4, params);
        if (accountRegister == null) {
            return R.result(RegisterResultEnum.DECODE_ERROR.ordinal());
        }
        AppProperties appProperties = propConfigService.getProp("sysProp");
        String username = accountRegister.getUN();
        String password = accountRegister.getPW();
        String deviceID = accountRegister.getDeviceID();
        int channelID = accountRegister.getCH();
        String phone = accountRegister.getPhone();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isNumeric(username) || StringUtils.isEmpty(phone)) {
            return R.result(RegisterResultEnum.DECODE_ERROR.ordinal());
        }
        deviceID = deviceID == null ? "" : deviceID;
        DeviceRegisterCount registerCount = null;
        if (appProperties.checkAccountRegister() == 1) {
            registerCount = FuncUtils.checkDeviceLimit(appProperties, registerMgrService, registerDeviceInfoService, channelID, deviceID);
            log.info("检查设备限制：username:{} channelId:{} deviceId:{} can:{} use:{}", username, channelID, deviceID, registerCount.getCanRegisterCount(), registerCount.getAlreadyRegisterCount());
            if (registerCount.getCanRegisterCount() <= 0) {
                return R.result(RegisterResultEnum.DEVICE_LIMIT.ordinal());
            }
        }
        int registerResult = FuncUtils.registerService(registerCount, loginInfoService, registerDeviceInfoService, phone, username, channelID, deviceID, RegisterTypeEnum.REGULAR.ordinal(), password);
        return R.result(registerResult);
    }

    R phoneRegByAuthCode(Map<Integer, Object> params) {
        PhoneRegisterDTO phoneRegister = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        RegisterDeviceInfoService registerDeviceInfoService = getParams(4, params);
        PhoneCountService phoneCountService = getParams(6, params);
        if (phoneRegister == null) {
            return R.result(RegisterResultEnum.DECODE_ERROR.ordinal());
        }
        if (StringUtils.isEmpty(phoneRegister.getAccount()) || StringUtils.isEmpty(phoneRegister.getPassword())
              || StringUtils.isEmpty(phoneRegister.getPhone())) {
            return R.result(RegisterResultEnum.DECODE_ERROR.ordinal());
        }
        AppProperties appProperties = propConfigService.getProp("sysProp");
        String phone = phoneRegister.getPhone();
        if (phone == null) {
            return R.result(RegisterResultEnum.DECODE_ERROR.ordinal());
        }

        if (phone.length() > 16 || phone.length() < 6) {

            return R.result(RegisterResultEnum.PHONE_LENGTH.ordinal());
        }
        /*
        LoginInfoDO loginInfo = loginInfoService.search(phone);
        if (loginInfo != null) {
            return R.result(RegisterResultEnum.PHONE_OVER_RANGE.ordinal());
        }
         */
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        //校验手机验证码
//        int result = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.REGISTER.ordinal(), phone, phoneRegister.getMessageCode());
//        if (result != RegisterResultEnum.SUCCESS.ordinal()){
//            return R.result(result);
//        }
        String deviceID = phoneRegister.getDeviceId() == null ? "" : phoneRegister.getDeviceId();
        int channelID = phoneRegister.getChannelId();
        String username = phoneRegister.getAccount();
        String password = phoneRegister.getPassword();
        PhoneCountDO phoneCountDO = null;
        if (appProperties.checkPhoneRegister() == 1) {//是否开启手机注册限制
            int limit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
            phoneCountDO = phoneCountService.get(phone);
            if (phoneCountDO == null) {
                phoneCountDO = new PhoneCountDO();
                phoneCountDO.setPhone(phone);
                phoneCountDO.setCount(0);
            }
            if (phoneCountDO.getCount() >= limit) {
                return R.result(RegisterResultEnum.PHONE_OVER_RANGE.ordinal());
            }
        }
        int registerResult = FuncUtils.registerService(null, loginInfoService, registerDeviceInfoService, phone, username, channelID, deviceID, RegisterTypeEnum.PHONE.ordinal(), password);
        if (registerResult == 0 && phoneCountDO != null) {
            //更新注册手机注册数量
            if (phoneCountDO.getCount() == 0) {
                phoneCountDO.setCount(1);
                phoneCountService.save(phoneCountDO);
            } else {
                phoneCountDO.setCount(phoneCountDO.getCount() + 1);
                phoneCountService.update(phoneCountDO);
            }
        }
        return R.result(registerResult);
    }

    R bindPhoneWithType(Map<Integer, Object> params) {
        BindPhoneDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        PhoneCountService phoneCountService = getParams(4, params);
        AppProperties appProperties = propConfigService.getProp("sysProp");
        int uid = data.getUid();
        String password = data.getPassword();
        String phone = data.getPhone();
        String messageCode = data.getMessageCode();
        // 绑定之前校验验证码真伪
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        int result = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.BIND.ordinal(), phone, messageCode);
        if (result == 0) {
            //根据ID检测玩家账号信息
            LoginInfoDO loginInfo = loginInfoService.get(uid);
            if (loginInfo == null) {
                return R.result(BindPhoneResultEnum.FAILED.ordinal());
            }
            // 校验输入的密码不对
            String hashPassword = CryptoUtils.getSHA256(password + loginInfo.getSalt(), "Base64");
            if (hashPassword != null && !hashPassword.equals(loginInfo.getPassword())) {
                return R.result(BindPhoneResultEnum.MISMATCH.ordinal());
            }
            // 本身是否已经绑定了手机号
            if (!StringUtils.isEmpty(loginInfo.getPhone())) {
                return R.result(BindPhoneResultEnum.FAILED.ordinal());
            }
//            注释   解绑后不能多次绑定的问题
//            LoginInfoDO otherInfo = loginInfoService.search(phone);
//            if (otherInfo != null){
//                return R.result(BindPhoneResultEnum.CONFLICT.ordinal());
//            }
            //注册手机加1
            if (appProperties.checkPhoneRegister() == 1) {
                PhoneCountDO phoneCountDO = phoneCountService.get(phone);
                if (phoneCountDO == null) {
                    phoneCountDO = new PhoneCountDO();
                    phoneCountDO.setCount(0);
                }
                int limit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
                if (phoneCountDO.getCount() >= limit) {
                    return R.result(BindPhoneResultEnum.PHONE_COUNT_BIND_MAX.ordinal());
                }
                phoneCountDO.setCount(phoneCountDO.getCount() + 1);
                phoneCountService.update(phoneCountDO);
            }
            loginInfo.setPhone(phone);
            loginInfoService.update(loginInfo);
            return R.result(BindPhoneResultEnum.SUCCESS.ordinal());
        } else {
            return R.result(result);
        }
    }

    R touristBindPhone(Map<Integer, Object> params) {
        TouristBindPhoneDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        PhoneCountService phoneCountService = getParams(4, params);
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        int uid = data.getUid();
        String phone = data.getPhone();
        String password = data.getPassword();
        String accountNumber = data.getAccountNumber();
        if (StringUtils.isNumeric(accountNumber)) {
            return R.result(BindPhoneResultEnum.FAILED.ordinal());
        }
        String messageCode = data.getMessageCode();
        int result = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.BIND.ordinal(), phone, messageCode);
        if (result == 0) {
            //根据ID检测玩家账号信息
            LoginInfoDO loginInfo = loginInfoService.get(uid);
            if (loginInfo == null) {
                return R.result(BindPhoneResultEnum.FAILED.ordinal());
            }
            // 本身是否已经绑定了手机号
            if (!StringUtils.isEmpty(loginInfo.getPhone())) {
                return R.result(BindPhoneResultEnum.FAILED.ordinal());
            }
            //检查设置的用户名是否被占用
            LoginInfoDO otherInfo = loginInfoService.search(accountNumber);
            if (otherInfo != null) {
                return R.result(BindPhoneResultEnum.USERNAME_CONFLICT.ordinal());
            }
            AppProperties appProperties = propConfigService.getProp("sysProp");
            if (appProperties.checkPhoneRegister() == 1) {
                //注册手机加1
                PhoneCountDO phoneCountDO = phoneCountService.get(phone);
                if (phoneCountDO == null) {
                    phoneCountDO = new PhoneCountDO();
                    phoneCountDO.setCount(0);
                }
                int limit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
                if (phoneCountDO.getCount() >= limit) {
                    return R.result(BindPhoneResultEnum.CONFLICT.ordinal());
                }
                phoneCountDO.setCount(phoneCountDO.getCount() + 1);
                phoneCountService.update(phoneCountDO);
            }
            //绑定手机号和账号
            loginInfo.setPhone(phone);
            loginInfo.setUserName(accountNumber);
            String salt = StringUtils.getRandomString(10);
            String _password = CryptoUtils.getSHA256(password + salt, "Base64");
            loginInfo.setPassword(_password);
            loginInfo.setSalt(salt);
            loginInfoService.update(loginInfo);
            return R.result(BindPhoneResultEnum.SUCCESS.ordinal()).put("phone", phone);
        } else {
            return R.result(result);
        }
    }

    R unBindPhone(Map<Integer, Object> params) {
        UnBindPhoneDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        PhoneCountService phoneCountService = getParams(4, params);
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        int uid = data.getUid();
        String phone = data.getPhone();
        String oldphone = data.getOldphone();
        String messageCode = data.getMessageCode();
        String oldmessageCode = data.getOldmessageCode();
        LoginInfoDO loginInfo = loginInfoService.get(uid);
        if (loginInfo == null || StringUtils.isEmpty(loginInfo.getPhone())) {
            return R.result(BindPhoneResultEnum.FAILED.ordinal());
        }
        String newPhone = loginInfo.getRegisterType() == RegisterTypeEnum.PHONE.ordinal() ? phone : "";
        int result = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.UNBIND.ordinal(), phone, messageCode);
        int result2 = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.UNBIND.ordinal(), oldphone, oldmessageCode);
        if (result == 0 && result2 == 0) {
//            loginInfo.setPhone("");
            loginInfo.setPhone(phone);
            loginInfoService.update(loginInfo);
            //注册手机减1
            AppProperties appProperties = propConfigService.getProp("sysProp");
            if (appProperties.checkPhoneRegister() == 1) {
                PhoneCountDO phoneCountDO = phoneCountService.get(oldphone);
                PhoneCountDO newPhoneCountDO = phoneCountService.get(phone);
                int limit = Integer.parseInt(appProperties.getDeviceRegisterLimit());
                if (newPhoneCountDO.getCount() >= limit) {
                    return R.result(RegisterResultEnum.PHONE_OVER_RANGE.ordinal());
                }
                if (newPhoneCountDO != null) {
                    phoneCountDO.setCount(newPhoneCountDO.getCount() + 1);
                    phoneCountService.update(newPhoneCountDO);
                } else {
                    newPhoneCountDO = new PhoneCountDO();
                    newPhoneCountDO.setPhone(phone);
                    newPhoneCountDO.setCount(1);
                    phoneCountService.save(newPhoneCountDO);
                }
                if (phoneCountDO != null) {
                    int count = Math.max(0, phoneCountDO.getCount() - 1);
                    phoneCountDO.setCount(count);
                    phoneCountService.update(phoneCountDO);
                }
            }
            log.info("更换绑定的手机号 UID {} oldphone{} newphone{} {}", uid, oldphone, newPhone);
            return R.result(BindPhoneResultEnum.SUCCESS.ordinal()).put("phone", phone);
        } else {
            if (result == 0) {
                return R.result(BindPhoneResultEnum.MESSAGE_CODE_ERROR_1.ordinal()).put("errorMsg", "新绑定手机验证码错误");
            } else {
                return R.result(BindPhoneResultEnum.MESSAGE_CODE_ERROR_2.ordinal()).put("errorMsg", "原绑定手机验证码错误");
            }
        }
    }

    R changePassword(Map<Integer, Object> params) {
        ChangePasswordDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        int uid = data.getUid();
        String newPassword = data.getNewPassword();
        if (!FuncUtils.isValidPassword(newPassword)) {
            return R.result(ResetInfoResultEnum.PASSWORD_UNQUALIFIED.ordinal());
        }
        LoginInfoDO loginInfo = loginInfoService.get(uid);
        if (loginInfo == null) {
            return R.result(ResetInfoResultEnum.NO_ACCOUNT.ordinal());
        }
        String _password = CryptoUtils.getSHA256(data.getOldPassword() + loginInfo.getSalt(), "Base64");
        if (!_password.equals(loginInfo.getPassword())) {
            return R.result(9);
        }
        SmsProperties smsProp = propConfigService.getProp("smsProp");
        if (data.getVerify() == null || data.getVerify().isEmpty()) {
            return R.error(LoginResultEnum.FAILED.ordinal()).put("errorMsg", "验证码不能为空");
        }
        int result = smsProp.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.RESET_PASSWORD.ordinal(), loginInfo.getPhone(), data.getVerify());
        if (result != RegisterResultEnum.SUCCESS.ordinal()) {
            return R.error(LoginResultEnum.FAILED.ordinal()).put("errorMsg", "验证码错误");
        }
        String salt = StringUtils.getRandomString(10);
        String hashPassword = CryptoUtils.getSHA256(newPassword + salt, "Base64");
        loginInfo.setPassword(hashPassword);
        loginInfo.setSalt(salt);
        loginInfoService.update(loginInfo);
        return R.result(ResetInfoResultEnum.SUCCESS.ordinal());
    }

    R resumeDelAccount(Map<Integer, Object> params) {
        Integer uid = getParams(0, params);
        AccountCancelRecordService accountCancelRecordService = getParams(1, params);
        AccountCancelRecordDO accountCancelRecordDO = accountCancelRecordService.getByUid(uid);
        if (accountCancelRecordDO == null) {
            return R.error(-1);
        }
        int overdueTime = 7 * 24 * 3600 * 1000;
        long desTime = System.currentTimeMillis() - accountCancelRecordDO.getCreateTime().getTime();
        if (desTime > overdueTime) {
            //超过恢复账号时间
            return R.error(-2);
        }
        //删除游戏的记录
        accountCancelRecordService.remove(accountCancelRecordDO.getId());
        return R.ok();
    }

    R phoneReset(Map<Integer, Object> params) {
        AdminOpDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        int uid = data.getUid();
        String phone = data.getPhone();
        LoginInfoDO loginInfo = loginInfoService.search(phone);
        if (loginInfo != null) {
            // 说明手机号被绑定过了
            return R.error(2);
        }
        loginInfo = loginInfoService.get(uid);
        if (loginInfo == null) {
            // 未找到该账号
            return R.error(1);
        }
        loginInfo.setPhone(phone);
        loginInfoService.update(loginInfo);
        return R.result(0);
    }

    ResponseEntity<Object> playerPasswordReset(Map<Integer, Object> params) {
        AdminOpDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        LimitIpService limitIpService = getParams(2, params);
        List<LimitIpDO> limitIps = limitIpService.list(null);
        String ip = getParams(3, params);
        if (!limitIps.stream().anyMatch(limitIp -> limitIp.getType() == 2 && limitIp.checkIp(ip))) {
            log.error("不在白名单之内  {} {}", ip, limitIps);
            return new ResponseEntity<>(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
        }
        int uid = data.getUid();
        String username = data.getUsername();
        LoginInfoDO loginInfo = null;
        if (uid > 0) {
            loginInfo = loginInfoService.get(uid);
        } else if (!StringUtils.isEmpty(username)) {
            loginInfo = loginInfoService.search(username);
        }
        if (loginInfo == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        String salt = StringUtils.getRandomString(10);
        String hashPassword = CryptoUtils.getSHA256(data.getPassword() + salt, "Base64");
        loginInfo.setPassword(hashPassword);
        loginInfo.setSalt(salt);
        //修改密码成功后，清理错误计数器
        errorInfos.remove(loginInfo.getUserName());
        log.info("重置密码 uid: {} oldPassword: {} oldSalt: {}", uid, loginInfo.getPassword(), loginInfo.getSalt());
        loginInfoService.updatePassword(loginInfo);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    ResponseEntity<AccountSearchDTO> findAccount(Map<Integer, Object> params) {
        AdminOpDTO data = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        int uid = data.getUid();
        String username = data.getUsername();
        LoginInfoDO loginInfoEntity = null;
        if (uid > 0) {
            loginInfoEntity = loginInfoService.get(uid);
        }
        if (!StringUtils.isEmpty(username)) {
            loginInfoEntity = loginInfoService.search(username);
        }
        if (loginInfoEntity == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        AccountSearchDTO accountSearchDTO = new AccountSearchDTO(loginInfoEntity.getId(), loginInfoEntity.getUserName());
        return new ResponseEntity<>(accountSearchDTO, HttpStatus.OK);
    }

    R reportDeviceInfo(Map<Integer, Object> params) {
        ReportDeviceDTO data = getParams(0, params);
        DeviceInfoService deviceInfoService = getParams(1, params);
        int channel = data.getChannel();
        String deviceId = data.getDeviceId();
        Map<String, Object> maps = new HashMap<>();
        maps.put("deviceId", deviceId);
        List<DeviceInfoDO> devices = deviceInfoService.list(maps);
        for (DeviceInfoDO device : devices) {
            if (device.getDeviceId().equals(deviceId) && device.getChannel() == channel) {
                return R.result(0);
            }
        }
        DeviceInfoDO device = new DeviceInfoDO();
        device.setDeviceId(deviceId);
        device.setChannel(channel);
        device.setDate(DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        deviceInfoService.save(device);
        return R.result(1);
    }

    R forgetPassword(Map<Integer, Object> params) {
        ForgetPasswordDTO forgetPasswordDTO = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        SmsService smsService = getParams(2, params);
        PropConfigService propConfigService = getParams(3, params);
        SmsProperties smsProperties = propConfigService.getProp("smsProp");
        if (forgetPasswordDTO == null) {
            return R.result(ResetInfoResultEnum.FAILED.ordinal());
        }

        String username = forgetPasswordDTO.getAccount();
        String messageCode = forgetPasswordDTO.getCode();
        String password = forgetPasswordDTO.getPassword();
        LoginInfoDO loginInfo = loginInfoService.search(username);
        if (loginInfo == null) {
            //未找到账号
            return R.result(6);
        }
        if (!FuncUtils.isValidPassword(password)) {
            return R.result(ResultCode.RestMessage_Code_1003.code);
        }
        if (StringUtils.isEmpty(loginInfo.getPhone())) {
            return R.result(ResultCode.RestMessage_Code_1002.code);
        }
        if (!StringUtils.equals(loginInfo.getPhone(), forgetPasswordDTO.getPhone())) {
            return R.result(ResultCode.RestMessage_Code_1001.code);
        }
        String phone = loginInfo.getPhone();
//        if (StringUtils.isEmpty(phone)){
//            //账号未绑定手机号
//            return R.result(-1);
//        }
        //校验验证码
//        int result = smsProperties.checkTest() ? 0 : smsService.verifyMessageCode(CodeTypeEnum.FORGET.ordinal(), phone, messageCode);
//        if (result == 0) {

        String salt = StringUtils.getRandomString(10);
        String hashPassword = CryptoUtils.getSHA256(password + salt, "Base64");
        loginInfo.setPassword(hashPassword);
        loginInfo.setSalt(salt);
        loginInfoService.update(loginInfo);
        //修改密码成功后，清理错误计数器
        errorInfos.remove(loginInfo.getUserName());
        return R.result(ResetInfoResultEnum.SUCCESS.ordinal());
//        }else{
//            return R.result(result);
//        }
    }

    String encrypt(String content, String key) {
        try {
            byte[] hexStr = HexUtils.fromHexString(key);
            //加密算法：AES/GCM/PKCS5Padding
            Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(hexStr, "AES");
            //随机生成iv 12位
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            //数据加密， AES-GCM-128
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(content.getBytes());          //数据加密
            //iv+加密数据 拼接  iv在前，加密数据在后
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            byte[] cipherMessage = byteBuffer.array();
            //转换为Base64 Base64算法有多种变体， 这里使用的是java.util.Base64
            return Base64.getEncoder().encodeToString(cipherMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    R realNameAuth(Map<Integer, Object> params) {
        RealNameAuthDTO data = getParams(0, params);
        RealNameProperties realNameProperties = getParams(1, params);
        LoginInfoService loginInfoService = getParams(2, params);
        HttpClientApi httpClientApi = getParams(3, params);
        if ("true".equals(realNameProperties.getTest())) {
            return R.ok();
        }
        int uid = data.getUid();
        String userName = data.getUserName();
        String idNum = data.getIdNum();
        String domain = "https://api.wlc.nppa.gov.cn/idcard/authentication/check";
        String bizId = realNameProperties.getBizId();
        String appId = realNameProperties.getAppId();
        String appName = realNameProperties.getAppName();
        String secretKey = realNameProperties.getSecretKey();
        Map<String, String> msg = new HashMap<>();
        msg.put("ai", appName + uid);
        msg.put("name", "" + userName);
        msg.put("idNum", "" + idNum);
        String msgStr = JSONUtils.beanToJson(msg);
        //log.info("用户实名认知明文:{}",msgStr);
        String encryptBodyDataStr = encrypt(msgStr, secretKey);
        msg.clear();
        msg.put("data", encryptBodyDataStr);
        String encryptBodyStr = JSONUtils.beanToJson(msg);
        //log.info("用户实名认知密文:{}",encryptBodyStr);
        long now = System.currentTimeMillis();
        String signContent = secretKey + "appId" + appId + "bizId" + bizId + "timestamps" + now + encryptBodyStr;
        String sign = CryptoUtils.getSHA256(signContent, "");
        //log.info("用户实名认证签名:{}",sign);
        try {
            Map<String, Object> result = httpClientApi.doRealLogic(domain, appId, bizId, now, sign, encryptBodyStr);
            Integer errorCode = getData("errcode", result);
            if (errorCode == 0) {
                Map<String, Object> map1 = getData("data", result);
                Map<String, Object> map2 = getData("result", map1);
                Integer status = getData("status", map2);
                String pi = getData("pi", map2);
                if (status == 0 && pi != null) {
                    LoginInfoDO loginInfoDO = loginInfoService.get(uid);
                    if (loginInfoDO != null) {
                        loginInfoDO.setPi(pi);
                        loginInfoService.update(loginInfoDO);
                        return R.ok().put("msg", "成功");
                    } else {
                        return R.error(-1).put("msg", "找不到用户");
                    }
                }
            } else {
                return R.error(-1).add(result);
            }
        } catch (Exception e) {
            log.error("实名认证错误", e.getMessage());
        }
        return R.error(-1).put("msg", "其他错误");
    }

    R gameReport(Map<Integer, Object> params) {
        GameReportDTO data = getParams(0, params);
        RealNameProperties realNameProperties = getParams(1, params);
        LoginInfoService loginInfoService = getParams(2, params);
        HttpClientApi httpClientApi = getParams(3, params);
        int uid = data.getUid();
        int op = data.getOp();
        LoginInfoDO loginInfoEntity = loginInfoService.get(uid);
        if (loginInfoEntity == null) {
            return R.error(-1).put("msg", "找不到用户");
        }
        if ("true".equals(realNameProperties.getTest())) {
            return R.ok();
        }
        List<ReportDataDTO> collections = new ArrayList<>();
        String pi = loginInfoEntity.getPi();
        String appName = realNameProperties.getAppName();
        String appId = realNameProperties.getAppId();
        String bizId = realNameProperties.getBizId();
        String secretKey = realNameProperties.getSecretKey();
        long time = System.currentTimeMillis();
        ReportDataDTO reportDataDTO = ReportDataDTO.builder()
                .pi(pi == null ? "" : pi)
                .no(1)
                .si(appName + uid)
                .bt(op)
                .ot(time / 1000)
                .ct(pi == null ? 0 : 2)
                .di(appName)
                .build();
        collections.add(reportDataDTO);
        Map<String, Object> msg = new HashMap<>();
        msg.put("collections", collections);
        String msgBodStr = JSONUtils.beanToJson(msg);
        log.info("用户行为数据上报明文:{}", msgBodStr);
        String encryptBodyDataStr = encrypt(msgBodStr, secretKey);
        Map<String, String> msg2 = new HashMap<>();
        msg2.put("data", encryptBodyDataStr);
        String encryptBodyStr = JSONUtils.beanToJson(msg2);
        log.info("用户行为数据上报密文:{}", encryptBodyStr);
        String signContent = secretKey + "appId" + appId + "bizId" + bizId + "timestamps" + time + encryptBodyStr;
        String sign = CryptoUtils.getSHA256(signContent, "");
        log.info("用户行为数据上报签名:{}", sign);
        String domain = "http://api2.wlc.nppa.gov.cn/behavior/collection/loginout";
        try {
            Map<String, Object> result = httpClientApi.doRealLogic(domain, appId, bizId, time, sign, encryptBodyStr);
            log.info("{} 游戏上报返回结果：{}", op == 1 ? "上线" : "下线", result);
        } catch (Exception e) {
            log.error("游戏上报错误{}", e.getMessage());
        }
        return R.ok();
    }

    R sendWarningMsg(Map<Integer, Object> params) {
        WarningMsgDTO data = getParams(0, params);
        SmsProperties smsProperties = getParams(1, params);
        SmsService smsService = getParams(2, params);
        String phone = smsProperties.getWarningPhone();
        if (StringUtils.isEmpty(phone)) {
            return R.error(-1, "未绑设置预警人手机号码");
        }
        if (smsProperties.checkTest()) {
            return R.ok();
        }
        List<String> strs = new ArrayList();
        strs.add(String.valueOf(data.getUid()));
        strs.add(data.getScore());
        strs.add(data.getType());
        return smsService.sendNotifyMessage(phone, strs, smsProperties);
    }

    R sendVerifyMsg(Map<Integer, Object> params) {
        SmsProperties smsProperties = getParams(3, params);
        if (smsProperties.checkTest()) {
            return R.ok();
        }
        String phone = getParams(0, params);
        String userName = getParams(1, params);
        int codeType = getParams(2, params);
        LoginInfoService loginInfoService = getParams(4, params);
        SmsService smsService = getParams(5, params);
        if (codeType == CodeTypeEnum.FORGET.ordinal()) {//忘记密码
            LoginInfoDO loginInfoDO = loginInfoService.search(userName);
            if (loginInfoDO == null) {
                log.info("输入的账号有误 {}", params.entrySet());
                return R.error(-1, "输入的账号有误");
            }
            phone = loginInfoDO.getPhone();
            if (StringUtils.isEmpty(phone)) {
                log.info("没有绑定的手机 {}", params.entrySet());
                return R.error(-1, "未绑定手机");
            }
        }
        return smsService.sendVerifyMessage(phone, codeType, smsProperties);
    }

    ResponseEntity<Object> backFrozen(Map<Integer, Object> params) {
        Integer type = getParams(0, params);
        Integer uid = getParams(1, params);
        LoginInfoService loginInfoService = getParams(2, params);
        LoginInfoDO loginInfoDO = loginInfoService.get(uid);
        if (loginInfoDO == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        loginInfoDO.setStatus(type);
        loginInfoService.update(loginInfoDO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    String clearPhoneCount(Map<Integer, Object> params) {
        String phone = getParams(0, params);
        PhoneCountService phoneCountService = getParams(1, params);
        Map<String, Object> req = new HashMap<>();
        req.put("phone", phone);
        List<PhoneCountDO> datas = phoneCountService.list(req);
        if (datas != null && datas.size() > 0) {
            String[] ids = datas.stream().map(PhoneCountDO::getPhone).toArray(String[]::new);
            phoneCountService.batchRemove(ids);
        }
        return "ok";
    }

    String clearRegisterCount(Map<Integer, Object> params) {
        String deviceId = getParams(0, params);
        RegisterDeviceInfoService registerDeviceInfoService = getParams(1, params);
        Map<String, Object> req = new HashMap<>();
        req.put("deviceId", deviceId);
        List<RegisterDeviceInfoDO> datas = registerDeviceInfoService.list(req);
        if (datas != null && datas.size() > 0) {
            int[] ids = datas.stream().mapToInt(RegisterDeviceInfoDO::getId).toArray();
            registerDeviceInfoService.batchRemove(ids);
        }
        return "ok";
    }

    String changeChannelByUid(Map<Integer, Object> params) {
        Integer uid = getParams(0, params);
        Integer old = getParams(1, params);
        Integer target = getParams(2, params);
        LoginInfoService loginInfoService = getParams(3, params);
        RolesService rolesService = getParams(4, params);
        LoginInfoDO loginInfoDO = loginInfoService.get(uid);
        if (loginInfoDO == null) {
            return "not find user";
        }
        if (loginInfoDO.getChannelId().intValue() != old.intValue()) {
            return "old channelId error ";
        }
        if (rolesService.updateChannel(uid, old, target)) {
            loginInfoDO.setChannelId(target);
            loginInfoService.update(loginInfoDO);
            return "ok";
        }
        return "fail";
    }

    String getRegisterIdByUid(Map<Integer, Object> params) {
        Integer uid = getParams(0, params);
        LoginInfoService loginInfoService = getParams(1, params);
        LoginInfoDO loginInfoDO = loginInfoService.get(uid);
        if (loginInfoDO == null) {
            return "not find user";
        }
        return loginInfoDO.getRegisterDeviceId() == null ? "" : loginInfoDO.getRegisterDeviceId();
    }

    String changeRegisterIdByUid(Map<Integer, Object> params) {
        Integer uid = getParams(0, params);
        String regId = getParams(1, params);
        LoginInfoService loginInfoService = getParams(2, params);
        RolesService rolesService = getParams(3, params);
        LoginInfoDO loginInfoDO = loginInfoService.get(uid);
        if (loginInfoDO == null) {
            return "not find user";
        }
        if (regId == null || regId.length() == 0) {
            return "regId is empty";
        }
        if (rolesService.updateRegisterDeviceId(uid, regId)) {
            loginInfoDO.setRegisterDeviceId(regId);
            loginInfoService.update(loginInfoDO);
            return "ok";
        }
        return "fail";
    }

    String getNotic(Map<Integer, Object> params) {
        return "fail";
    }

}
