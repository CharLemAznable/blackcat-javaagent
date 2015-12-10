package com.github.bingoohuang.blackcat.javaagent.instrument;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatClientInterceptor;
import com.google.common.base.Throwables;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class BlackcatJavaAgent {
    private static int counter;

    public static void premain(
            String agentArgs, Instrumentation instrumentation
    ) throws InstantiationException {
//        ++counter;
//        final String callbackId = String.valueOf(counter);
        try {
//            if (agentArgs == null) {
            agentArgs = BlackcatClientInterceptor.class.getName();
//                throw new IllegalArgumentException(
//                        "Agent argument is required of the form " +
//                                "'interceptor-class-name[;interceptor-custom-args]'");
//            }
//            String[] tokens = agentArgs.split(";", 2);
//            Class<?> clazz = BlackcatJavaAgent.class.getClassLoader().loadClass(tokens[0]);
//            BlackcatJavaAgentInterceptor interceptor;
//            interceptor = (BlackcatJavaAgentInterceptor) clazz.newInstance();
//            interceptor.init(tokens.length == 2 ? tokens[1] : null);
//            BlackcatJavaAgentCallback.registerCallback(callbackId, interceptor);

            instrumentation.addTransformer(new AgentTransformer());
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    static class AgentTransformer implements ClassFileTransformer {
        public byte[] transform(final ClassLoader loader,
                                final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                final byte[] classfileBuffer)
                throws IllegalClassFormatException {

            if (!isAncestor(BlackcatJavaAgent.class.getClassLoader(), loader))
                return classfileBuffer;

            return AccessController.doPrivileged(new PrivilegedAction<byte[]>() {
                public byte[] run() {
                    try {
                        BlackcatInstrument blackcatInst;
                        blackcatInst = new BlackcatInstrument(classfileBuffer);
                        return blackcatInst.modifyClass().y;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw Throwables.propagate(e);
                    }
                }
            });
        }
    }

    private static boolean isAncestor(ClassLoader ancestor, ClassLoader cl) {
        if (ancestor == null || cl == null) return false;
        if (ancestor.equals(cl)) return true;

        return isAncestor(ancestor, cl.getParent());
    }
}
