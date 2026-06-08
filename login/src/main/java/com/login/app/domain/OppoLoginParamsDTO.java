package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class OppoLoginParamsDTO extends BaseLoginParamsDTO{
    private OppoCheckInfo checkInfo;
}
