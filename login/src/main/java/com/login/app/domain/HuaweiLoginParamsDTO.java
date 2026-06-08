package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class HuaweiLoginParamsDTO extends BaseLoginParamsDTO{
    private HuaweiCheckInfo checkInfo;
}
