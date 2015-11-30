package com.github.bingoohuang.blackcat.javaagent.instrument;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentCallback;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentInterceptor;
import com.github.bingoohuang.blackcat.javaagent.callback.Cat;
import com.github.bingoohuang.blackcat.javaagent.utils.Asms;
import com.github.bingoohuang.blackcat.javaagent.utils.Debugs;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.p;
import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.sig;
import static com.github.bingoohuang.blackcat.javaagent.utils.TreeAsms.*;
import static org.objectweb.asm.Opcodes.*;

public class BlackcatInstrument {
    protected final BlackcatJavaAgentInterceptor interceptor = BlackcatJavaAgentCallback.INSTANCE;
    protected final String className;
    protected final byte[] classFileBuffer;

    protected ClassNode classNode;
    protected Type classType;

    protected MethodNode methodNode;
    protected Type[] methodArgs;
    protected Type methodReturnType;
    protected int methodOffset;

    protected LabelNode startNode;

    public BlackcatInstrument(
            String className, byte[] classFileBuffer) {
        this.className = className;
        this.classFileBuffer = classFileBuffer;
    }

    public byte[] modifyClass() {
        classNode = new ClassNode();
        ClassReader cr = new ClassReader(classFileBuffer);
        cr.accept(classNode, 0);
        classType = Type.getType("L" + classNode.name + ";");

        boolean ok = interceptor.interceptClass(classNode, className);
        if (!ok) return classFileBuffer;

        int count = modifyMethodCount(classNode.methods);
        if (count == 0) return classFileBuffer;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);

        byte[] bytes = cw.toByteArray();

        Debugs.writeClassFile(classNode, className, bytes);

        return bytes;
    }

    private int modifyMethodCount(List<MethodNode> methods) {
        int transformedCount = 0;
        for (MethodNode node : methods) {
            if (modifyMethod(node)) ++transformedCount;
        }

        return transformedCount;
    }

    private boolean modifyMethod(MethodNode mn) {
        if (Helper.isAbstract(mn)) return false;
        if (!interceptor.interceptMethod(classNode, mn)) return false;

        methodNode = mn;
        methodArgs = Type.getArgumentTypes(methodNode.desc);
        methodReturnType = Type.getReturnType(methodNode.desc);
        methodOffset = Helper.isStatic(methodNode) ? 0 : 1;

        addTraceStart();
        addTraceReturn();
        addTraceThrow();
        addTraceThrowableUncaught();

        return true;
    }

    private void addTraceStart() {
        InsnList insnList = new InsnList();
        insnList.add(new LdcInsnNode(className));
        insnList.add(new LdcInsnNode(Asms.describeMethod(methodNode, false)));
        insnList.add(getPushInst(methodArgs.length));
        insnList.add(new TypeInsnNode(ANEWARRAY, p(Object.class)));
        for (int i = 0; i < methodArgs.length; i++) {
            insnList.add(new InsnNode(DUP));
            insnList.add(getPushInst(i));
            insnList.add(getLoadInst(methodArgs[i], getArgumentPosition(i)));
            MethodInsnNode mNode = getWrapperCtorInst(methodArgs[i]);
            if (mNode != null) insnList.add(mNode);
            insnList.add(new InsnNode(AASTORE));
        }

        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(Cat.class),
                "start",
                sig(void.class, String.class, String.class, Object[].class),
                false));

        startNode = new LabelNode();
        methodNode.instructions.insert(startNode);
        methodNode.instructions.insert(insnList);
    }

    private void addTraceReturn() {
        InsnList insnList = methodNode.instructions;

        Iterator<AbstractInsnNode> it = insnList.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insnNode = it.next();

            switch (insnNode.getOpcode()) {
                case RETURN:
                    insnList.insertBefore(insnNode, getVoidReturnTraceInsts());
                    break;
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case ARETURN:
                case DRETURN:
                    insnList.insertBefore(insnNode, getReturnTraceInsts());
            }
        }
    }

    private void addTraceThrow() {
        InsnList insnList = methodNode.instructions;

        Iterator<AbstractInsnNode> it = insnList.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insnNode = it.next();

            switch (insnNode.getOpcode()) {
                case ATHROW:
                    insnList.insertBefore(insnNode, getThrowTraceInsts());
                    break;
            }
        }
    }

    private void addTraceThrowableUncaught() {
        InsnList insnList = methodNode.instructions;

        LabelNode endNode = new LabelNode();
        insnList.add(endNode);

        addCatchBlock(startNode, endNode);

    }

    private void addCatchBlock(LabelNode startNode, LabelNode endNode) {
        InsnList insnList = new InsnList();

        LabelNode handlerNode = new LabelNode();
        insnList.add(handlerNode);

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));
        methodOffset++;

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(Cat.class), "uncaught",
                sig(void.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new InsnNode(ATHROW));

        methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
                startNode, endNode, handlerNode, "java/lang/Throwable"));
        methodNode.instructions.add(insnList);
    }

    private InsnList getVoidReturnTraceInsts() {
        InsnList insnList = new InsnList();
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(Cat.class), "finish",
                sig(void.class), false));

        return insnList;
    }

    private InsnList getThrowTraceInsts() {
        InsnList insnList = new InsnList();

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));

        methodOffset++;
        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(Cat.class), "caught",
                sig(void.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));

        return insnList;
    }

    private InsnList getReturnTraceInsts() {
        InsnList insnList = new InsnList();

        int returnedVariablePosition = getFistAvailablePosition();
        insnList.add(getStoreInst(methodReturnType, returnedVariablePosition));

        updateMethodOffset(methodReturnType);
        insnList.add(getLoadInst(methodReturnType, returnedVariablePosition));
        MethodInsnNode mNode = getWrapperCtorInst(methodReturnType);
        if (mNode != null) insnList.add(mNode);
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                p(Cat.class), "finish",
                sig(void.class, Object.class), false));

        insnList.add(getLoadInst(methodReturnType, returnedVariablePosition));

        return insnList;
    }

    private int getFistAvailablePosition() {
        return methodNode.maxLocals + methodOffset;
    }

    protected void updateMethodOffset(Type type) {
        char charType = type.getDescriptor().charAt(0);
        methodOffset += (charType == 'J' || charType == 'D') ? 2 : 1;
    }

    public int getArgumentPosition(int argNo) {
        return Helper.getArgPosition(methodOffset, methodArgs, argNo);
    }
}
