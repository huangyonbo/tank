package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class TencentCheckInfo {

    private String openid;
    private String payToken;
    private String pf;
    private String pfkey;
    private String userType;
    private String accessToken;
}
