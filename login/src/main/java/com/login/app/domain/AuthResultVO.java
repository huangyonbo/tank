package com.login.app.domain;

import com.login.enums.LoginResultEnum;
import lombok.Data;

@Data
public class AuthResultVO {
    private int code = LoginResultEnum.FAILED.ordinal();
    private String message;
    private String name;
    private int sex;
    private String headUrl;
    private int channelId;
    private String phone;
    private String recruiter;
    private String payInfo;
    private String extend;//扩展参数
    private String ip;
}
