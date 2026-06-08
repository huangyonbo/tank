package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class AccountRegisterDTO {
    private String UN;
    private String PW;
    private String Phone;
    private int CH;
    private String version;
    private String deviceID;
}
