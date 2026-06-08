package com.login.app.service.impl;

import com.login.app.service.SmsService;
import com.login.common.redis.shiro.RedisManager;
import com.login.common.utils.CryptoUtils;
import com.login.common.utils.JSONUtils;
import com.login.common.utils.R;
import com.login.common.utils.StringUtils;
import com.login.enums.RegisterResultEnum;
import com.login.http.HttpClientApi;
import com.login.prop.domain.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author yanghuajun
 * @date 2021-05-03
 * @description
 */

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private HttpClientApi httpClientApi;

    @Autowired
    private RedisManager redisManager;
    public enum CodeTypeEnum {
        BIND(19508561),               // 绑定手机号;
        UNBIND(19517488),             // 解绑手机号;
        RESET_PASSWORD(19511173),     // 修改密码;
        FORGET(19511173),             // 忘记密码
        REGISTER(19511173),           // 账号注册;
        WARNING(19511173), // 预警信息;或用于账号验证 ;
        END(0);
        int value;
        CodeTypeEnum(int value) {
            this.value = value;
        }
        public static int  getValue(Integer value) {
            return Arrays.stream(values()).filter(s -> s.ordinal() == value).findAny().orElse(END).value;
        }

    }

    @Override
    public R sendVerifyMessage(String phone, int codeType,SmsProperties smsProperties) {
        String code     = StringUtils.getMessageCode();
        String nonce    = StringUtils.getRandomString(32);
        String curTime  = String.valueOf(System.currentTimeMillis() / 1000);
        String checkSum = CryptoUtils.getCheckSum(smsProperties.getAppSecret(),nonce,curTime);
        String url = "https://api.netease.im/sms/sendcode.action";
//        int verifyTemplateId = Integer.parseInt(smsProperties.getVerifyTemplateId());
        int verifyTemplateId = CodeTypeEnum.getValue(codeType);
        Map<String,Object> result = httpClientApi.doVerify(url, smsProperties.getAppKey(), nonce, curTime, checkSum, phone,verifyTemplateId, code);
        int _code = (int)result.get("code");
        if (_code == 200){
            String codeKey = phone + "_" + codeType;
            redisManager.set(codeKey,code,600000);//设置10分钟过期 10 * 60 * 1000
            log.info("返回 {}  codekey {} {}",_code,codeKey,code);
            return R.ok();
        }else{
            log.info("erro mesg  {}",result.get("msg").toString());
            return R.error(_code,result.get("msg").toString());
        }
    }

    @Override
    public R sendNotifyMessage(String phone, List<String> params,SmsProperties smsProperties) {
        String nonce    = StringUtils.getRandomString(32);
        String curTime  = String.valueOf(System.currentTimeMillis());
        String checkSum = CryptoUtils.getCheckSum(smsProperties.getAppSecret(),nonce,curTime);
        String url = "https://api.netease.im/sms/sendtemplate.action";
        String _params = params == null ? "[]" : JSONUtils.beanToJson(params);
        int notifyTemplateId = Integer.parseInt(smsProperties.getNotifyTemplateId());
        Map<String,Object> result = httpClientApi.doNotify(url, smsProperties.getAppKey(), nonce, curTime, checkSum, phone,notifyTemplateId,_params);
        int _code = (int)result.get("code");
        if (_code == 200){
            return R.ok();
        }else{
            return R.error(-1,"系统错误");
        }
    }

    @Override
    public int verifyMessageCode(int codeType, String phone, String messageCode) {
        // 校验存起来的验证码
        String codeKey = phone + "_" + codeType;
        String codeValue = redisManager.get(codeKey);
        log.info("验证码比对 key {} {} {}",codeKey,codeValue,messageCode);
        if (codeValue == null) {
            // 已经过期
            return RegisterResultEnum.MESSAGE_CODE_EXPIRE.ordinal();
        }
        if (!codeValue.equals(messageCode)){
            // 验证码不对
            return RegisterResultEnum.MESSAGE_CODE_ERROR.ordinal();
        } else {
            // 校验成功,删除验证码
            redisManager.del(codeKey);
        }
        return RegisterResultEnum.SUCCESS.ordinal();
    }
}
