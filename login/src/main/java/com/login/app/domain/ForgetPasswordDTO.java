package com.login.app.domain;

import lombok.Data;

@Data
public class ForgetPasswordDTO {
    private String account;
    private String password;
    private String code;
    private String phone;
    private int channel;
}
