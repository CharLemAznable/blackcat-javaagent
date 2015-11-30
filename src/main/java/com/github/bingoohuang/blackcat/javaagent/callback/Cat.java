package com.github.bingoohuang.blackcat.javaagent.callback;

public class Cat {
    static ThreadLocal<BlackcatMethodRt> tl = new InheritableThreadLocal<BlackcatMethodRt>();

    public static void start(Class<?> clazz, String methodDesc, Object... params) {
        start(clazz.getName(), methodDesc, params);
    }

    public static void start(String className, String methodDesc, Object... params) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt rt = instance.doStart(className, methodDesc, params);
        tl.set(rt);
    }

    public static void finish() {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt rt = tl.get();
        instance.doVoidFinish(rt);
    }

    public static void finish(Object result) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt rt = tl.get();
        instance.doFinish(rt, result);
    }

    public static void uncaught(Throwable throwable) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt rt = tl.get();
        instance.doThrowableUncaught(rt, throwable);
    }


    public static void caught(Throwable throwable) {
        BlackcatJavaAgentCallback instance = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt rt = tl.get();
        instance.doThrowableCaught(rt, throwable);
    }

}
