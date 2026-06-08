package com.login.app.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Lambda
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class TouristBindPhoneDTO extends BindPhoneDTO{
    private String accountNumber;
}
