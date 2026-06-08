package com.login.enums;

/**
 * @Author: Lambda
 * @Description: 手机验证码类型
 */
public enum CodeTypeEnum {
    BIND,               // 绑定手机号;
    UNBIND,             // 解绑手机号;
    RESET_PASSWORD,     // 修改密码;
    FORGET,             // 忘记密码
    REGISTER,           // 账号注册;
    WARNING,            // 预警信息;或用于账号验证
}
