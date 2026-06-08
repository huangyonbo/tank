package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class ChannelLoginInfoDTO {
    private String userId;
    private String appName;
    private int channelId;
    private String version;
}
