package com.github.bingoohuang.blackcat.javaagent.callback;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class BlackcatMethodRt {
    public final String executionId;
    public final String pid = getPid();
    public final long startMillis = System.currentTimeMillis();
    public long endMillis;
    public long costMillis;

    public final String className;
    public final String methodDesc;
    public final Object[] args;
    public Throwable throwableCaught;
    public Object result;
    public Throwable throwableUncaught;
    public boolean sameThrowable = false;

    public static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0]; // --> 742912@localhost
    }

    public BlackcatMethodRt(
            String executionId,
            String className,
            String methodDesc,
            Object[] args) {
        this.executionId = executionId;
        this.className = className;
        this.methodDesc = methodDesc;
        this.args = args;
    }

    public void setThrowableCaught(Throwable throwableCaught) {
        this.throwableCaught = throwableCaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setThrowableUncaught(Throwable throwableUncaught) {
        this.throwableUncaught = throwableUncaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void finishExecute() {
        this.endMillis = System.currentTimeMillis();
        this.costMillis = endMillis - startMillis;
    }

    @Override
    public String toString() {
        return "BlackcatMethodRt{" +
                "executionId='" + executionId + '\'' +
                ", pid='" + pid + '\'' +
                ", startMillis=" + startMillis +
                ", endMillis=" + endMillis +
                ", costMillis=" + costMillis +
                ", className=" + className +
                ", methodDesc=" + methodDesc +
                ", args=" + Arrays.toString(args) +
                ", throwableCaught=" + throwableCaught +
                ", result=" + result +
                ", throwableUncaught=" + throwableUncaught +
                ", sameThrowable=" + sameThrowable +
                '}';
    }
}
