package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description: 客户端埋点上报设备号
 */

@Data
public class ReportDeviceDTO {
    private String deviceId;
    private int channel;
}
