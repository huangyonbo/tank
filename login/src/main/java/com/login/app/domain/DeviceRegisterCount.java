package com.login.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRegisterCount {
    private int canRegisterCount;
    private int alreadyRegisterCount;
    private RegisterDeviceInfoDO registerDeviceInfoDO;
}
