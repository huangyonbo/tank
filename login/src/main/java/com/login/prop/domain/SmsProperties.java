package com.login.prop.domain;

import lombok.Data;

@Data
public class SmsProperties {
    private String test;
    private String warningPhone;
    private String appKey;
    private String appSecret;
    private String verifyTemplateId;
    private String notifyTemplateId;

    public boolean checkTest(){
        return "true".equals(test);
    }
}
