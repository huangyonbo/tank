package com.login.app.domain;

import lombok.Data;

@Data
public class ErrorInfo {
    private long time = 0;
    private int count = 0;
}
