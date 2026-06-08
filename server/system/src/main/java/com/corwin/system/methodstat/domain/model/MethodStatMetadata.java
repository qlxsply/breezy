package com.corwin.system.methodstat.domain.model;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
public record MethodStatMetadata(
        MethodStatKey key,
        String packageName,
        String className,
        String methodName,
        String methodSignature,
        boolean methodSwitchEnabled
) {

    public MethodStatMetadata {
        Objects.requireNonNull(key, "key required");
        Objects.requireNonNull(packageName, "packageName required");
        Objects.requireNonNull(className, "className required");
        Objects.requireNonNull(methodName, "methodName required");
        Objects.requireNonNull(methodSignature, "methodSignature required");
    }

    public MethodStatMetadata withMethodSwitchEnabled(boolean nextMethodSwitchEnabled) {
        return new MethodStatMetadata(key, packageName, className, methodName, methodSignature,
                nextMethodSwitchEnabled);
    }

}
