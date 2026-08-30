package com.corwin.system.methodstat.infrastructure.aop;

import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodDescriptor;
import com.corwin.system.methodstat.infrastructure.key.MethodStatKeyStrategy;
import java.lang.reflect.Method;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Resolves a {@link MethodStatMethodDescriptor} from a declared method and target class,
 * determining the actual implementation class and building the method key.
 *
 * @author Corwin 2026/3/25
 */
@Component
@RequiredArgsConstructor
public class MethodStatAnnotationResolver {

  private final MethodStatKeyStrategy keyStrategy;

  /**
   * Resolve a method descriptor by identifying the implementation class and method.
   *
   * @param declaredMethod the originally declared method
   * @param targetClass the target class (may be a proxy)
   * @return the resolved method descriptor
   */
  public MethodStatMethodDescriptor resolve(Method declaredMethod, Class<?> targetClass) {
    Objects.requireNonNull(declaredMethod, "declaredMethod required");

    Class<?> implementationClass = resolveImplementationClass(declaredMethod, targetClass);
    Method implementationMethod = resolveImplementationMethod(implementationClass, declaredMethod);

    String methodSignature =
        MethodStatMethodDescriptor.buildMethodSignature(implementationClass, implementationMethod);
    MethodStatKey key = keyStrategy.buildKey(methodSignature);
    return new MethodStatMethodDescriptor(
        implementationClass, implementationMethod, declaredMethod, key);
  }

  private Class<?> resolveImplementationClass(Method declaredMethod, Class<?> targetClass) {
    if (targetClass == null) {
      return declaredMethod.getDeclaringClass();
    }
    return ClassUtils.getUserClass(targetClass);
  }

  private Method resolveImplementationMethod(Class<?> implementationClass, Method declaredMethod) {
    Method method = findPublicMethod(implementationClass, declaredMethod);
    if (method != null) {
      return method;
    }
    return declaredMethod;
  }

  private Method findPublicMethod(Class<?> owner, Method method) {
    try {
      return owner.getMethod(method.getName(), method.getParameterTypes());
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }
}
