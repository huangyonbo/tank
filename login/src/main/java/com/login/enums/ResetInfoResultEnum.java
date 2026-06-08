package com.login.enums;

/**
 * @author yanghuajun
 * @date 2021-05-03
 * @description 重置数据返回码
 */
public enum ResetInfoResultEnum {
    SUCCESS,                // 成功
    FAILED,                 // 失败
    NO_ACCOUNT,             // 账号未注册
    MESSAGE_CODE_ERROR,     // 验证码不正确
    MESSAGE_CODE_EXPIRE,    // 验证码超时
    PHONE_ERROR,            // 手机号输入错误
    OLD_PASSWORD_WRONG,     // 旧密码错误
    TIMEOUT,                 // 页面等待超时
    PASSWORD_UNQUALIFIED     // 密码不符合要求
}
