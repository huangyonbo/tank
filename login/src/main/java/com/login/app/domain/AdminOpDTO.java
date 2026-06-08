package com.login.app.domain;

import lombok.Data;

@Data
public class AdminOpDTO {
    private int uid;
    private String username;
    private String phone;
    private String password;
}
