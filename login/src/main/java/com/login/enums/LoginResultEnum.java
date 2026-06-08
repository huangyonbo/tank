package com.login.enums;

/**
 * @Author: Lambda
 * @Description:
 */
public enum LoginResultEnum {
    SUCCESS,               // 0-登录成功
    FAILED,                // 1-登录失败
    MIS_MATCH,             // 2-账号密码不匹配
    NO_ACCOUNT,            // 3-账号未注册
    SDK_TYPE_ERROR,        // 4-sdk标志错误
    NOT_IN_WHITE_LIST,     // 5-不在白名单
    MAINTENANCE,           // 6-
    DEVICE_LIMIT,          // 7-设备限制
    DECODE_ERROR,          // 8-数据解析错误
    DECODE_ERROR1,          //9-ip黑名单
    DECODE_ERROR2,          //10-服务器处于维护状态
    USER_CANCLE,            //11-用户主动注销
    SYS_FROZEN,             //12-系统封禁
    DEVICE_ID_FAIL, //13-登录设备号变更
}
