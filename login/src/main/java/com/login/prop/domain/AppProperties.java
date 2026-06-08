package com.login.prop.domain;

import lombok.Data;

@Data
public class AppProperties {

    /**
     * 密码加密默认盐值
     */
    private String salt;
    /**
     * 默认地址(本机)
     */
    private String address;
    /**
     * 昵称前缀
     */
    private String nickName;
    /**
     * 手机号注册（1 开启，0关闭）
     */
    private String phoneRegister;
    /**
     * 账号注册（1 开启，0关闭）
     */
    private String accountRegister;
    /**
     * 注册设备限制
     */
    private String deviceRegisterLimit;


    public int checkPhoneRegister(){
        return "true".equals(phoneRegister) ? 1: 0;
    }

    public int checkAccountRegister(){
        return "true".equals(accountRegister) ? 1: 0;
    }
}
