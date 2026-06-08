package com.corwin.system.methodstat.domain.model;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodDescriptor(
        Class<?> implementationClass,
        Method implementationMethod,
        Method declaredMethod,
        MethodStatKey key
) {

    public MethodStatMethodDescriptor {
        Objects.requireNonNull(implementationClass, "implementationClass required");
        Objects.requireNonNull(implementationMethod, "implementationMethod required");
        Objects.requireNonNull(declaredMethod, "declaredMethod required");
        Objects.requireNonNull(key, "key required");
    }

    public String packageName() {
        return implementationClass.getPackageName();
    }

    public String className() {
        return implementationClass.getSimpleName();
    }

    public String methodName() {
        return implementationMethod.getName();
    }

    public String methodSignature() {
        return buildMethodSignature(implementationClass, implementationMethod);
    }

    public static String buildMethodSignature(Class<?> implementationClass, Method implementationMethod) {
        String parameterTypes = Arrays.stream(implementationMethod.getParameterTypes()).map(Class::getTypeName)
                .collect(Collectors.joining(","));
        return implementationClass.getName() + "#" + implementationMethod.getName() + "(" + parameterTypes + ")";
    }

}
