package com.github.bingoohuang.blackcat.javaagent;

import com.github.bingoohuang.blackcat.javaagent.annotations.BlackcatMonitor;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentCallback;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        while (true) {
            Main main = new Main();
            main.voidMethod("abc");
            main.intMethod();

            Main.staticVoidMethod();

            Blackcats.sleep(10, TimeUnit.SECONDS);
        }
    }

    @BlackcatMonitor
    public void voidMethod(String var) {
        System.out.println(var + "voidMethod");
        Blackcats.sleep(new Random().nextInt(4), TimeUnit.SECONDS);
    }

    @BlackcatMonitor
    public long intMethod() {
        System.out.println(System.nanoTime());
        System.out.println("intMethod");
        Blackcats.sleep(new Random().nextInt(4), TimeUnit.SECONDS);
        System.out.println(System.nanoTime());
        return System.currentTimeMillis();
    }

    @BlackcatMonitor
    public static void staticVoidMethod() {
        System.out.println("staticVoidMethod");
        Blackcats.sleep(new Random().nextInt(4), TimeUnit.SECONDS);
    }

    public void voidMethod12(String var) throws Throwable {
        Object[] var3 = new Object[]{var};
        Class[] var4 = new Class[]{String.class};
        Object var5 = Helper.getSource(Main.class, "voidMethod", var4);
        BlackcatJavaAgentCallback var6 = BlackcatJavaAgentCallback.getInstance();
        BlackcatMethodRt var7 = var6.doStart(var5, var3);

        try {
            System.out.println(var + "voidMethod");
            var6.doVoidFinish(var7);
        } catch (Throwable var10) {
            var6.doThrowableUncaught(var7, var10);
            throw var10;
        }
    }
}
