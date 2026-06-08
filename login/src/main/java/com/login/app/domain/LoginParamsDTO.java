package com.login.app.domain;

import lombok.Data;

@Data
public class LoginParamsDTO {
    private String userName;
    private String password;
    private String deviceId;
    private String realIp;
    private String verify;
    private String phone;
    private int sdkType;
    private String version;
}
