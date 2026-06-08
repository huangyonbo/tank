package com.login.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSearchDTO {

    /**
     * 玩家ID
     */
    private Integer uid;
    /**
     * 账号
     */
    private String username;

}
