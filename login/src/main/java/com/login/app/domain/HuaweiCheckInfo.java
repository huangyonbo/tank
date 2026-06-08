package com.login.app.domain;

import lombok.Data;

/**
 * @Author: Lambda
 * @Description:
 */

@Data
public class HuaweiCheckInfo {
    private String ts;
    private String playerId;
    private String playerLevel;
    private String gameAuthSign;
}
