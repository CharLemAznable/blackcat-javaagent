package com.github.bingoohuang.blackcat.javaagent.callback;


public abstract class BlackcatJavaAgentCallback {
    public static final BlackcatClientInterceptor INSTANCE = new BlackcatClientInterceptor();

    final ThreadLocal<Integer> COUNTER = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 1;
        }
    };

    private final String getExecutionId() {
        int counter = COUNTER.get();
        COUNTER.set(counter + 1);

        return Thread.currentThread().getId() + ":" + counter;
    }

    public static BlackcatJavaAgentCallback getInstance() {
        return INSTANCE;
    }

    public final BlackcatMethodRt doStart(
            String className,
            String methodName,
            String methodDesc,
            Object[] args) {
        String executionId = getExecutionId();
        BlackcatMethodRt rt = new BlackcatMethodRt(executionId, className, methodName, methodDesc, args);
        try {
            onStart(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }

        return rt;
    }

    public final void doThrowableCaught(BlackcatMethodRt rt, Throwable throwableCaught) {
        if (rt.throwableCaught == throwableCaught) return;

        try {
            rt.setThrowableCaught(throwableCaught);
            onThrowableCaught(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    public final void doThrowableUncaught(BlackcatMethodRt rt, Throwable throwable) {
        try {
            rt.finishExecute();
            rt.setThrowableUncaught(throwable);
            onThrowableUncaught(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    public final void doVoidFinish(BlackcatMethodRt rt) {
        doFinish(rt, "<void>");
    }

    public final void doFinish(BlackcatMethodRt rt, Object result) {
        try {
            rt.finishExecute();
            rt.setResult(result);
            onFinish(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    protected abstract void onStart(BlackcatMethodRt rt);

    protected abstract void onThrowableCaught(BlackcatMethodRt rt);

    protected abstract void onThrowableUncaught(BlackcatMethodRt rt);

    protected abstract void onFinish(BlackcatMethodRt rt);
}
