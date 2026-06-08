package com.login.app.domain;

import lombok.Data;

@Data
public class PhoneRegisterDTO {
    private String phone;
    private String password;
    private int channelId;
    private String deviceId;
    private String account;
}
