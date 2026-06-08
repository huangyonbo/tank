package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class ChangePasswordDTO {
    private int uid;
    private String oldPassword;
    private String newPassword;
    private String verify;
    private String oldverify;
}
