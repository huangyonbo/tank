package com.login.system.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author bootdo 1992lcg@163.com
 * @version V1.0
 */
@Data
public class UserToken implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private String username;
    private String name;
    private String password;
    private Long deptId;
}
