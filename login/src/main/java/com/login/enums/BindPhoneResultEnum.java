package com.login.enums;

/**
 * @author yanghuajun
 * @date 2021-05-03
 * @description 绑定手机号返回结果
 */
public enum BindPhoneResultEnum {
    SUCCESS,                    // 绑定号码成功;
    FAILED,                     // 绑定号码失败
    MISMATCH,                   // 账号密码不匹配
    NO_ACCOUNT,                 // 账号不存在
    CONFLICT,                   // 该号码绑定的账号 超过最大账号绑定数量
    MESSAGE_CODE_ERROR,         // 验证码不正确
    USERNAME_CONFLICT,          // 账号已被占用
    MESSAGE_CODE_EXPIRE,        // 验证码过期
    PHONE_COUNT_BIND_MAX ,       // 手机号绑定达到上限
    MESSAGE_CODE_ERROR_1,         // 旧手机验证码不正确
    MESSAGE_CODE_ERROR_2,         // 旧手机验证码不正确

}
