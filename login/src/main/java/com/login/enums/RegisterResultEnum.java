package com.login.enums;

/**
 * @Author: Lambda
 * @Description:
 */
public enum RegisterResultEnum {
    SUCCESS,                // 注册成功;
    FAILED,                 // 注册失败;
    CONFLICT,               // 账号冲突;
    PHONE_OVER_RANGE,       // 手机号绑定的账号超过上限
    MESSAGE_CODE_ERROR,     // 验证码不正确;
    MESSAGE_CODE_EXPIRE,    // 验证码过期;
    DEVICE_LIMIT,           // 设备限制;
    DECODE_ERROR,           // 数据解密失败
    PHONE_LENGTH,           // 长度不对
    OVER_RANGE              // 最大限制
}
