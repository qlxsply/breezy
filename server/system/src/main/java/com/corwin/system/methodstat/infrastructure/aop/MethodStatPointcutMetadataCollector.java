package com.corwin.system.methodstat.infrastructure.aop;

import com.corwin.system.methodstat.application.service.MethodStatMetadataCollector;
import com.corwin.system.methodstat.application.service.MethodStatMetadataRegistrationAppService;
import com.corwin.system.methodstat.domain.model.MethodStatMethodDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link MethodStatMetadataCollector} that scans all beans in the
 * application context and registers metadata for methods matching the configured pointcut.
 * @author Corwin 2026/4/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MethodStatPointcutMetadataCollector implements MethodStatMetadataCollector {

    private final ApplicationContext applicationContext;
    private final MethodStatPointcutProvider pointcutProvider;
    private final MethodStatAnnotationResolver annotationResolver;
    private final MethodStatMetadataRegistrationAppService metadataRegistrationAppService;

    /**
     * Iterate over all registered beans, match methods against the pointcut, and register
     * their metadata for statistics collection.
     * @return the number of methods successfully registered
     */
    @Override
    public int collectAllPointcutMetadata() {
        Map<String, MethodStatMethodDescriptor> descriptors = new LinkedHashMap<>();
        Pointcut pointcut = pointcutProvider.pointcut();
        ClassFilter classFilter = pointcut.getClassFilter();
        MethodMatcher methodMatcher = pointcut.getMethodMatcher();

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) {
                continue;
            }

            Class<?> targetClass = ClassUtils.getUserClass(beanType);
            if (!classFilter.matches(targetClass)) {
                continue;
            }

            for (Method candidateMethod : targetClass.getMethods()) {
                Method resolvedMethod = resolveInvocableMethod(targetClass, candidateMethod);
                if (resolvedMethod == null || !Modifier.isPublic(resolvedMethod.getModifiers())) {
                    continue;
                }
                boolean matched = methodMatcher.matches(candidateMethod, targetClass);
                if (!matched && !resolvedMethod.equals(candidateMethod)) {
                    matched = methodMatcher.matches(resolvedMethod, targetClass);
                }
                if (!matched) {
                    continue;
                }
                addDescriptor(descriptors, resolvedMethod, targetClass);
            }
        }

        descriptors.values().forEach(metadataRegistrationAppService::register);
        return descriptors.size();
    }

    private Method resolveInvocableMethod(Class<?> targetClass, Method candidateMethod) {
        if (!candidateMethod.getDeclaringClass().isInterface()) {
            return candidateMethod;
        }
        try {
            return targetClass.getMethod(candidateMethod.getName(), candidateMethod.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            log.debug("Method stat metadata collect skipped missing method, class={}, method={}", targetClass.getName(),
                    candidateMethod);
            return null;
        }
    }

    private void addDescriptor(Map<String, MethodStatMethodDescriptor> descriptors, Method declaredMethod,
            Class<?> targetClass) {
        try {
            MethodStatMethodDescriptor descriptor = annotationResolver.resolve(declaredMethod, targetClass);
            descriptors.putIfAbsent(descriptor.key().value(), descriptor);
        } catch (RuntimeException ex) {
            log.warn("Method stat metadata collect failed, method={}", declaredMethod, ex);
        }
    }
}
