package com.login.app.domain;

import lombok.Data;

@Data
public class LoginAuthParamsDTO {
    private int uid;
    private String token;
    private String extend;//扩展参数
}
