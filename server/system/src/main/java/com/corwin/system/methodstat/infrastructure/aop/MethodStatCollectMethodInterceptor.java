package com.corwin.system.methodstat.infrastructure.aop;

import com.corwin.system.methodstat.application.service.MethodStatMetadataRegistrationAppService;
import com.corwin.system.methodstat.application.service.MethodStatSwitchAppService;
import com.corwin.system.methodstat.domain.model.MethodStatInvocationEvent;
import com.corwin.system.methodstat.domain.model.MethodStatMethodDescriptor;
import com.corwin.framework.event.publisher.AsyncEventPublisher;
import com.corwin.framework.util.HighDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP method interceptor that collects invocation statistics for methods matching the pointcut.
 * It checks the global and per-method switches, registers metadata, measures execution duration,
 * and publishes an event for asynchronous aggregation.
 * @author Corwin 2026/4/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MethodStatCollectMethodInterceptor implements MethodInterceptor {

    private final MethodStatAnnotationResolver annotationResolver;
    private final MethodStatMetadataRegistrationAppService metadataRegistrationAppService;
    private final MethodStatSwitchAppService switchAppService;
    private final AsyncEventPublisher eventPublisher;

    /**
     * Intercept the method invocation, record timing and result, and publish a statistics event.
     * @param invocation the method invocation
     * @return the result of the intercepted method
     * @throws Throwable if the intercepted method throws
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!switchAppService.isGlobalEnabled()) {
            return invocation.proceed();
        }

        Method declaredMethod = invocation.getMethod();
        Class<?> targetClass = resolveTargetClass(invocation, declaredMethod);
        MethodStatMethodDescriptor descriptor = annotationResolver.resolve(declaredMethod, targetClass);
        var metadata = metadataRegistrationAppService.register(descriptor);
        if (!switchAppService.isCollectEnabled(metadata.key())) {
            return invocation.proceed();
        }

        long startedAt = HighDate.realTimestampMillis();
        boolean success = false;
        Throwable throwable = null;
        try {
            Object result = invocation.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            long finishedAt = HighDate.realTimestampMillis();
            long durationMillis = Math.max(0L, finishedAt - startedAt);
            MethodStatInvocationEvent event = new MethodStatInvocationEvent(metadata.key(), startedAt, finishedAt,
                    durationMillis, success, throwable == null ? null : throwable.getClass().getName());
            publishSafely(event);
        }
    }

    private Class<?> resolveTargetClass(MethodInvocation invocation, Method declaredMethod) {
        Object target = invocation.getThis();
        if (target == null) {
            return declaredMethod.getDeclaringClass();
        }
        return target.getClass();
    }

    private void publishSafely(MethodStatInvocationEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException ex) {
            log.warn("Method stat event publish failed, key={}", event.key(), ex);
        }
    }
}
