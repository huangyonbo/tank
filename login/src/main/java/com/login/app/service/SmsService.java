package com.login.app.service;


import com.login.common.utils.R;
import com.login.prop.domain.SmsProperties;

import java.util.List;

public interface SmsService {

    /**
     * 发送短信
     *
     * @param phone 手机号
     * @param codeType 短信类型
     * @return 发送结果
     */
    R sendVerifyMessage(String phone, int codeType, SmsProperties smsProperties);

    /**
     * 发送短信
     * @param phone 手机号
     * @param params 模板中的参数
     * @return 发送结果
     */
    R sendNotifyMessage(String phone, List<String> params, SmsProperties smsProperties);

    /**
     * 校验短信验证码
     *
     * @param codeType 短信类型
     * @param phone 手机号
     * @param messageCode 短信验证码
     * @return 校验结果
     */
    int verifyMessageCode(int codeType, String phone, String messageCode);
}
