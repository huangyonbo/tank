package com.login.enums;

public enum ResultCode {
    RestMessage_Code_1001(1001,"密保无法匹配"),
    RestMessage_Code_1002(1002,"未绑定密保"),
    RestMessage_Code_1003(1003,"密码或密保不符合要求"),
    Node(99999,"未知");

    public int  code;
     public String message;
    ResultCode(int i, String value) {
         code=i;
         message=value;
    }
}
