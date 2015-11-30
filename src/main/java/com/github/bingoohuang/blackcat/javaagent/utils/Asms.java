package com.github.bingoohuang.blackcat.javaagent.utils;

import com.github.bingoohuang.blackcat.sdk.utils.StrBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.getObjectType;

public class Asms {

    public static String describeMethod(MethodNode mn, boolean withThrows) {
        StrBuilder description = new StrBuilder();

        if ((mn.access & ACC_PUBLIC) != 0) description.p("public ");
        if ((mn.access & ACC_PRIVATE) != 0) description.p("private ");
        if ((mn.access & ACC_PROTECTED) != 0) description.p("protected ");
        if ((mn.access & ACC_STATIC) != 0) description.p("static ");
        if ((mn.access & ACC_ABSTRACT) != 0) description.p("abstract ");
        if ((mn.access & ACC_SYNCHRONIZED) != 0) description.p("synchronized ");

        Type returnType = Type.getReturnType(mn.desc);
        description.p(simpleClassName(returnType.getClassName())).p(' ');
        description.p(mn.name);

        Type[] argumentTypes = Type.getArgumentTypes(mn.desc);
        description.p('(');
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argumentType = argumentTypes[i];
            if (i > 0) description.p(", ");
            String className = argumentType.getClassName();
            String simpleClassName = simpleClassName(className);
            description.p(simpleClassName);
        }
        description.p(')');

        if (withThrows) {
            @SuppressWarnings("unchecked")
            List<String> thrownInternalClassNames = mn.exceptions;
            if (!thrownInternalClassNames.isEmpty()) {
                description.p(" throws ");
                for (String internalClassName : thrownInternalClassNames) {
                    description.p(getObjectType(internalClassName).getClassName());
                    description.p(", ");
                }
                description.deleteLast();
            }
        }

        return description.toString();
    }

    private static String simpleClassName(String className) {
        int pos = -1;
        char[] chars = className.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (c == '.' || c == '/') pos = i;
        }
        return pos > 0 ? className.substring(pos + 1) : className;
    }


    public static boolean isAnyMethodAnnPresent(
            List<MethodNode> methods,
            Class<? extends Annotation> annotationClass) {
        for (MethodNode mn : methods) {
            if (isAnnPresent(mn, annotationClass)) return true;
        }

        return false;
    }

    public static boolean isAnnPresent(
            ClassNode cn,
            Class<? extends Annotation> annotationClass) {
        List<AnnotationNode> visibleAnnotations = cn.visibleAnnotations;
        return isAnnPresent(annotationClass, visibleAnnotations);
    }

    public static boolean isAnnPresent(
            MethodNode mn,
            Class<? extends Annotation> annotationClass) {
        List<AnnotationNode> visibleAnnotations = mn.visibleAnnotations;
        return isAnnPresent(annotationClass, visibleAnnotations);
    }

    public static boolean isAnnPresent(
            Class<? extends Annotation> annotationClass,
            List<AnnotationNode> visibleAnnotations) {
        String expectedDesc = ci(annotationClass);
        return isAnnPresent(expectedDesc, visibleAnnotations);
    }

    public static boolean isAnnPresent(
            String annClassId,
            List<AnnotationNode> visibleAnnotations) {
        if (visibleAnnotations == null) return false;

        for (AnnotationNode visibleAnn : visibleAnnotations) {
            if (annClassId.equals(visibleAnn.desc)) return true;
        }

        return false;
    }


    public static boolean isWildAnnPresent(
            String wildAnnClassId,
            List<AnnotationNode> visibleAnns) {
        if (visibleAnns == null) return false;

        for (AnnotationNode visibleAnn : visibleAnns) {
            Type annType = Type.getType(visibleAnn.desc);
            String annClassName = annType.getClassName();
            if (wildcardMatch(annClassName, wildAnnClassId)) return true;
        }

        return false;
    }

    // Creates a dotted class name from a path/package name
    public static String c(String p) {
        return p.replace('/', '.');
    }

    // Creates a class path name, from a Class.
    public static String p(Class n) {
        return n.getName().replace('.', '/');
    }

    public static String p(String className) {
        return className.replace('.', '/');
    }


    // Creates a class identifier of form Labc/abc;, from a Class.
    public static String ci(Class n) {
        if (n.isArray()) {
            n = n.getComponentType();
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "[B";
                } else if (n == Boolean.TYPE) {
                    return "[Z";
                } else if (n == Short.TYPE) {
                    return "[S";
                } else if (n == Character.TYPE) {
                    return "[C";
                } else if (n == Integer.TYPE) {
                    return "[I";
                } else if (n == Float.TYPE) {
                    return "[F";
                } else if (n == Double.TYPE) {
                    return "[D";
                } else if (n == Long.TYPE) {
                    return "[J";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "[" + ci(n);
            }
        } else {
            if (n.isPrimitive()) {
                if (n == Byte.TYPE) {
                    return "B";
                } else if (n == Boolean.TYPE) {
                    return "Z";
                } else if (n == Short.TYPE) {
                    return "S";
                } else if (n == Character.TYPE) {
                    return "C";
                } else if (n == Integer.TYPE) {
                    return "I";
                } else if (n == Float.TYPE) {
                    return "F";
                } else if (n == Double.TYPE) {
                    return "D";
                } else if (n == Long.TYPE) {
                    return "J";
                } else if (n == Void.TYPE) {
                    return "V";
                } else {
                    throw new RuntimeException("Unrecognized type in compiler: " + n.getName());
                }
            } else {
                return "L" + p(n) + ";";
            }
        }
    }

    // Create a method signature from the given param types and return values
    public static String sig(Class retval, Class... params) {
        return sigParams(params) + ci(retval);
    }

    public static String sig(Class[] retvalParams) {
        Class[] justParams = new Class[retvalParams.length - 1];
        System.arraycopy(retvalParams, 1, justParams, 0, justParams.length);
        return sigParams(justParams) + ci(retvalParams[0]);
    }

    public static String sig(Class retval, String descriptor, Class... params) {
        return sigParams(descriptor, params) + ci(retval);
    }

    public static String sigParams(Class... params) {
        StringBuilder signature = new StringBuilder("(");

        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }

        signature.append(")");

        return signature.toString();
    }

    public static String sigParams(String descriptor, Class... params) {
        StringBuilder signature = new StringBuilder("(");

        signature.append(descriptor);

        for (int i = 0; i < params.length; i++) {
            signature.append(ci(params[i]));
        }

        signature.append(")");

        return signature.toString();
    }

}