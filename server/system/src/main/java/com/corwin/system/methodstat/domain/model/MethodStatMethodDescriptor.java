package com.corwin.system.methodstat.domain.model;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Domain model describing a method intercepted for statistics collection, holding its
 * implementation class, reflection Method references, and unique key.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatMethodDescriptor(
    Class<?> implementationClass,
    Method implementationMethod,
    Method declaredMethod,
    MethodStatKey key) {

  public MethodStatMethodDescriptor {
    Objects.requireNonNull(implementationClass, "implementationClass required");
    Objects.requireNonNull(implementationMethod, "implementationMethod required");
    Objects.requireNonNull(declaredMethod, "declaredMethod required");
    Objects.requireNonNull(key, "key required");
  }

  /**
   * @return the package name of the implementation class
   */
  public String packageName() {
    return implementationClass.getPackageName();
  }

  /**
   * @return the simple class name of the implementation class
   */
  public String className() {
    return implementationClass.getSimpleName();
  }

  /**
   * @return the name of the implementation method
   */
  public String methodName() {
    return implementationMethod.getName();
  }

  /**
   * @return the full method signature including class name, method name, and parameter types
   */
  public String methodSignature() {
    return buildMethodSignature(implementationClass, implementationMethod);
  }

  /**
   * Build a canonical method signature string for the given class and method.
   *
   * @param implementationClass the class declaring the method
   * @param implementationMethod the method
   * @return the formatted method signature
   */
  public static String buildMethodSignature(
      Class<?> implementationClass, Method implementationMethod) {
    String parameterTypes =
        Arrays.stream(implementationMethod.getParameterTypes())
            .map(Class::getTypeName)
            .collect(Collectors.joining(","));
    return implementationClass.getName()
        + "#"
        + implementationMethod.getName()
        + "("
        + parameterTypes
        + ")";
  }
}
