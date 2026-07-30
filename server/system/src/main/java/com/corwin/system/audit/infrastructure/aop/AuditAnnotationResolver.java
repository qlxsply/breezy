package com.corwin.system.audit.infrastructure.aop;

import com.corwin.framework.util.StrUtil;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.resource.published.ApiMeta;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves audit metadata from {@link Audit} annotations at runtime.
 * Caches resolved {@link AuditOperation} instances keyed by method and
 * implementation class to avoid repeated reflection lookups.
 *
 * @author Corwin 2026/4/19
 */
@Component
public class AuditAnnotationResolver {

    private final ConcurrentMap<CacheKey, Optional<AuditOperation>> cache = new ConcurrentHashMap<>();

    /**
     * Resolves the audit operation metadata for a given method and target class.
     * Checks for {@link Audit} and {@code ApiMeta} annotations, caches the result,
     * and returns a resolved {@link AuditOperation} or {@code null} if no annotation is present.
     *
     * @param declaredMethod the method being invoked
     * @param targetClass    the actual runtime class of the target (may be a CGLIB proxy)
     * @return the resolved audit operation, or {@code null} if not annotated
     */
    public AuditOperation resolve(Method declaredMethod, Class<?> targetClass) {
        Objects.requireNonNull(declaredMethod, "declaredMethod required");

        Class<?> implementationClass = resolveImplementationClass(declaredMethod, targetClass);
        CacheKey cacheKey = new CacheKey(declaredMethod, implementationClass);
        return cache.computeIfAbsent(cacheKey, this::resolveOperation).orElse(null);
    }

    private Optional<AuditOperation> resolveOperation(CacheKey cacheKey) {
        Method declaredMethod = cacheKey.declaredMethod();
        Class<?> implementationClass = cacheKey.implementationClass();
        Method implementationMethod = resolveImplementationMethod(implementationClass, declaredMethod);

        Audit annotation = findAuditAnnotation(implementationMethod, declaredMethod, implementationClass);
        if (annotation == null) {
            return Optional.empty();
        }

        ApiMeta apiMeta = findApiMeta(implementationClass, declaredMethod);
        AuditResource resource = annotation.resource();
        AuditAction action = annotation.action();
        String description = buildDescription(annotation.description(), resource, action);
        AuditLevel level = annotation.level() == null ? AuditLevel.MEDIUM : annotation.level();
        String applicationCode = apiMeta == null ? null : apiMeta.module().code();

        return Optional.of(new AuditOperation(resource.itemCode(), action.itemCode(), description, level,
                annotation.recordRequest(),
                annotation.recordResponse(), applicationCode));
    }

    private Audit findAuditAnnotation(Method implementationMethod, Method declaredMethod,
            Class<?> implementationClass) {
        Audit annotation = AnnotatedElementUtils.findMergedAnnotation(implementationMethod, Audit.class);
        if (annotation != null) {
            return annotation;
        }
        annotation = AnnotatedElementUtils.findMergedAnnotation(declaredMethod, Audit.class);
        if (annotation != null) {
            return annotation;
        }
        annotation = AnnotatedElementUtils.findMergedAnnotation(implementationClass, Audit.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(declaredMethod.getDeclaringClass(), Audit.class);
    }

    private ApiMeta findApiMeta(Class<?> implementationClass, Method declaredMethod) {
        ApiMeta apiMeta = AnnotatedElementUtils.findMergedAnnotation(implementationClass, ApiMeta.class);
        if (apiMeta != null) {
            return apiMeta;
        }
        return AnnotatedElementUtils.findMergedAnnotation(declaredMethod.getDeclaringClass(), ApiMeta.class);
    }

    private Class<?> resolveImplementationClass(Method declaredMethod, Class<?> targetClass) {
        if (targetClass == null) {
            return declaredMethod.getDeclaringClass();
        }
        return ClassUtils.getUserClass(targetClass);
    }

    private Method resolveImplementationMethod(Class<?> implementationClass, Method declaredMethod) {
        try {
            return implementationClass.getMethod(declaredMethod.getName(), declaredMethod.getParameterTypes());
        } catch (NoSuchMethodException ignored) {
            return declaredMethod;
        }
    }

    private String buildDescription(String rawDescription, AuditResource resource, AuditAction action) {
        String normalized = StrUtil.trimToNull(rawDescription);
        if (normalized != null) {
            return normalized;
        }
        return resource.label() + action.label();
    }

    private record CacheKey(
            Method declaredMethod,
            Class<?> implementationClass
    ) {
    }
}
