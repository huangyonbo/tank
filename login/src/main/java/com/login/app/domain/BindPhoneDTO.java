package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class BindPhoneDTO {
    private int uid;
    private String phone;
    private String password;
    private String messageCode;
}
