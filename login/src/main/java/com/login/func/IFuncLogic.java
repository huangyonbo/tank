package com.login.func;

public interface IFuncLogic {
    boolean init();
    void register(int runId, String methodName);
    Object execute(int runId, Object... params);
}
