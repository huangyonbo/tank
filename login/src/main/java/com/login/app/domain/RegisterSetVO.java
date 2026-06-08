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
public class RegisterSetVO {
    private Integer phone_register; // 手机号注册 1开 0关
    private Integer account_register; // 账号注册 1开 0关
    private Integer register_limit; // 注册设备限制数量
}
