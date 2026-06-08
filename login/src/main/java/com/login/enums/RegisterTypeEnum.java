package com.login.enums;

/**
 * @Author: Lambda
 * @Description:
 */
public enum RegisterTypeEnum {
    REGULAR,               // 0-常规注册，个性账号密码
    QUICK,                 // 1-快速注册/快速登录
    PHONE,                 // 2-手机号注册
    OAUTH,                 // 3-第三方账号注册
    QUICK_CHANNEL,         // 4-渠道包快速登录
    WECHAT,                // 5-微信登录
    ROBOT                  // 6-机器人测试
}
