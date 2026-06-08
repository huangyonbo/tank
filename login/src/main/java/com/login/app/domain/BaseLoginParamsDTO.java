package com.login.app.domain;

import lombok.Data;

@Data
public class BaseLoginParamsDTO {
    private ChannelLoginInfoDTO loginInfo;
    private String deviceID;
    private String clientIP;
    private String version;
    private String sdkVersion;
}
