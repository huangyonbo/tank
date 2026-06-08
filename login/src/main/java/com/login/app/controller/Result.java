package com.login.app.controller;

/**
 * @Author HYB
 * @Date 2026/3/18
 * @Time 15:19
 * @Desc
 */
public class Result {
    protected boolean success;
    protected int code;
    protected String msg;

    public static Result create(String msg){
        Result result = new Result();
        result.msg = msg;
        return result;
    }

    public static Result create(int code, String msg){
        return new Result(code,msg);
    }


    public static Result create(){
        return new Result();
    }

    private Result(int code, String msg) {
        this.success = false;
        this.code = code;
        this.msg = msg;
    }


    public Result() {
        this.success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
