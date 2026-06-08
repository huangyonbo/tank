package com.login.app.domain;

import lombok.Data;

@Data
public class QuickCheckInfo {
    private String uid;
    private String token;
    private String iosFlag;
    private int subChannel;//聚合SDK返回的渠道方的编号
}
