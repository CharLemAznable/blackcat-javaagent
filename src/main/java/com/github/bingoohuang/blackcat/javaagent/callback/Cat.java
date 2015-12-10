package com.github.bingoohuang.blackcat.javaagent.callback;

public class Cat {
    private BlackcatMethodRt rt;

    public void start(
            Class<?> clazz,
            String methodName,
            String methodDesc,
            Object... params) {
        start(clazz.getName(), methodName, methodDesc, params);
    }

    public void start(
            String className,
            String methodName,
            String methodDesc,
            Object... params) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        rt = instance.doStart(className, methodName, methodDesc, params);
    }

    public void finish() {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        instance.doVoidFinish(rt);
    }

    public void finish(Object result) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        instance.doFinish(rt, result);
    }

    public void uncaught(Throwable throwable) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        instance.doThrowableUncaught(rt, throwable);
    }

    public void caught(Throwable throwable) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        instance.doThrowableCaught(rt, throwable);
    }

}
