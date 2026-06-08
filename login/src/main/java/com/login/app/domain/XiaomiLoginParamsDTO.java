package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class XiaomiLoginParamsDTO extends BaseLoginParamsDTO{
    private XiaomiCheckInfo checkInfo;
}
