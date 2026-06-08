package com.login.app.domain;

import lombok.Data;

@Data
public class WechatLoginParamsDTO extends BaseLoginParamsDTO{
    private WechatCheckInfo checkInfo;
}
