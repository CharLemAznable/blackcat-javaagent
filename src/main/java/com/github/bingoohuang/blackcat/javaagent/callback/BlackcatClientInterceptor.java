package com.github.bingoohuang.blackcat.javaagent.callback;

import com.github.bingoohuang.blackcat.javaagent.annotations.BlackcatMonitor;
import com.github.bingoohuang.blackcat.javaagent.discruptor.BlackcatClient;
import com.github.bingoohuang.blackcat.javaagent.utils.Asms;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.*;
import static com.github.bingoohuang.blackcat.sdk.utils.Blackcats.readDiamond;
import static com.github.bingoohuang.blackcat.sdk.utils.Blackcats.splitLinesWoComments;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

public class BlackcatClientInterceptor
        extends BlackcatJavaAgentInterceptorAdapter {
    Logger log = LoggerFactory.getLogger(BlackcatClientInterceptor.class);

    @Override
    public boolean interceptClass(ClassNode classNode, String className) {
        Class<BlackcatMonitor> annClass = BlackcatMonitor.class;
        return isAnnPresent(classNode, annClass)
                || isAnyMethodAnnPresent(classNode.methods, annClass)
                || isClassConfigured(className, classNode.methods);
    }

    @Override
    public boolean interceptMethod(ClassNode classNode, MethodNode methodNode) {
        return isAnnPresent(methodNode, BlackcatMonitor.class)
                || isMethodConfigured(methodNode);
    }

    @Override
    protected void onThrowableUncaught(BlackcatMethodRt rt) {
        log.debug("onThrowableUncaught:{}", rt);
        BlackcatClient.send(rt);
    }

    @Override
    protected void onFinish(BlackcatMethodRt rt) {
        log.debug("onThrowableUncaught:{}" + rt);
        BlackcatClient.send(rt);
    }

    private boolean isClassConfigured(
            String className, // com/github/bingoohuang/springbootbank
            List<MethodNode> methodNodes) {
        String config = readDiamond("blackcat^interceptClasses");
        if (StringUtils.isEmpty(config)) return false;

        String dottedClassName = className.replace('/', '.');
        List<String> interceptClasses = splitLinesWoComments(config, "#");
        for (String interceptClass : interceptClasses) {
            if (wildcardMatch(dottedClassName, interceptClass)) return true;
        }

        return false;
    }

    private boolean isMethodConfigured(MethodNode methodNode) {
        String config = readDiamond("blackcat^interceptMethods");
        if (StringUtils.isEmpty(config)) return false;

        List<String> interceptMethods = splitLinesWoComments(config, "#");
        for (String interceptMethod : interceptMethods) {
            if (interceptMethod.startsWith("@")) {
                String wildAnnClassId = interceptMethod.substring(1);
                List visibleAnns = methodNode.visibleAnnotations;
                if (isWildAnnPresent(wildAnnClassId, visibleAnns)) return true;
            } else {
                String methodTypeString = Asms.describeMethod(methodNode, false);

                log.debug("interceptMethod:{}, methodTypeString:{}",
                        interceptMethod, methodTypeString);

                if (wildcardMatch(methodTypeString, interceptMethod)) {
                    return true;
                }
            }
        }

        return false;
    }

}
