package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class UnBindPhoneDTO {
    private int uid;
    private String oldphone;
    private String phone;
    private String username;
    private String messageCode;
    private String oldmessageCode;
}
