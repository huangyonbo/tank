package com.login.func;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFuncLogic implements IFuncLogic{

    private Map<Integer, MethodCallBackData> handlers = new HashMap<>();

    protected  <T> T getParams(int index,Map<Integer,Object> params){
        Object obj = params.get(index);
        return obj == null ? null : (T)obj;
    }

    protected  <T> T getData(String key,Map<String,Object> params){
        Object obj = params.get(key);
        return obj == null ? null : (T)obj;
    }

    @Override
    public void register(int runId, String methodName) {
        Class<?> type = this.getClass();
        if (handlers.containsKey(runId)) {
            String error = type.getSimpleName() + " had  registed runId = " + runId + ",methodName=" + methodName;
            throw new RuntimeException(error);
        }
        MethodAccess access = MethodAccess.get(type);
        int methodIndex = access.getIndex(methodName);
        MethodCallBackData handler = MethodCallBackData.builder()
                .access(access).methodIndex(methodIndex).listener(this)
                .build();
        handlers.put(runId,handler);
    }

    @Override
    public Object execute(int runId, Object... params) {
        MethodCallBackData handler = handlers.get(runId);
        if (handler != null){
            return handler.invoke(params);
        }
        String error = " not find handler runId = " + runId;
        throw new RuntimeException(error);
    }
}
