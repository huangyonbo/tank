package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class VivoLoginParamsDTO extends BaseLoginParamsDTO{
    private VivoCheckInfo checkInfo;
}
