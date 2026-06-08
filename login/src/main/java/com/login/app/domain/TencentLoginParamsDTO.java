package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class TencentLoginParamsDTO extends BaseLoginParamsDTO{
    private TencentCheckInfo checkInfo;
}
