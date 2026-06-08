package com.login.app.domain;

import lombok.Data;

@Data
public class QuickLoginParamsDTO extends BaseLoginParamsDTO{
    private QuickCheckInfo checkInfo;
}
