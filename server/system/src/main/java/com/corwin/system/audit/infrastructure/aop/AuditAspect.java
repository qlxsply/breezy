package com.corwin.system.audit.infrastructure.aop;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.SysException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.audit.application.command.AuditRecordCommand;
import com.corwin.system.audit.application.service.AuditLogService;
import com.corwin.system.audit.config.SystemAuditConfigSpecs;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/**
 * Captures audited method execution and delegates persistence to {@link AuditLogService}.
 *
 * @author Corwin 2026/7/30
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditAnnotationResolver annotationResolver;
    private final AuditPayloadSanitizer payloadSanitizer;
    private final AuditLogService auditLogService;

    public AuditAspect(AuditAnnotationResolver annotationResolver, AuditPayloadSanitizer payloadSanitizer,
            AuditLogService auditLogService) {
        this.annotationResolver = annotationResolver;
        this.payloadSanitizer = payloadSanitizer;
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(com.corwin.system.audit.published.Audit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = resolveTargetClass(joinPoint, method);
        AuditOperation operation = annotationResolver.resolve(method, targetClass);
        var auditPolicy = Configs.snapshot(SystemAuditConfigSpecs.AUDIT_POLICY).value();
        if (operation == null || !auditPolicy.enabled()) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = currentRequest();
        Instant startedAt = HighDate.mockInstant();
        Object result = null;
        Throwable throwable = null;
        boolean success = false;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            Instant endedAt = HighDate.mockInstant();
            long durationMs = Math.max(0L, HighDate.betweenMillis(startedAt, endedAt));
            auditLogService.record(new AuditRecordCommand(CtxUtil.getTraceId(), readRequestId(request), currentUserId(),
                    currentUsername(), currentUserType(), operation.applicationCode(), ApiProtocol.HTTP,
                    resolveHttpMethod(request), resolvePathPattern(request), resolveRequestUri(request),
                    serializePermissionCodes(), operation.resource(), operation.action(), operation.description(),
                    operation.level(), CtxUtil.getClientIp(), readUserAgent(request),
                    operation.recordRequest() ? payloadSanitizer.summarizeRequestParameters(
                            request == null ? null : request.getParameterMap(), auditPolicy) : null,
                    operation.recordRequest() ? payloadSanitizer.summarizeRequestBody(joinPoint.getArgs(), auditPolicy)
                            : null,
                    success && operation.recordResponse()
                            ? payloadSanitizer.summarizeResponseBody(result, auditPolicy) : null,
                    success, resolveErrorCode(throwable),
                    throwable == null ? null : payloadSanitizer.summarizeErrorMessage(throwable, auditPolicy),
                    startedAt, endedAt, durationMs, endedAt));
        }
    }

    private Class<?> resolveTargetClass(ProceedingJoinPoint joinPoint, Method method) {
        Object target = joinPoint.getTarget();
        return target == null ? method.getDeclaringClass() : target.getClass();
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String readRequestId(HttpServletRequest request) {
        return request == null ? null : StrUtil.trimToNull(request.getHeader("X-Request-Id"));
    }

    private String readUserAgent(HttpServletRequest request) {
        return request == null ? null : StrUtil.trimToNull(request.getHeader("User-Agent"));
    }

    private ApiMethod resolveHttpMethod(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String method = StrUtil.trimToNull(request.getMethod());
        if (method == null) {
            return null;
        }
        try {
            return ApiMethod.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ApiMethod.ANY;
        }
    }

    private String resolvePathPattern(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (attribute instanceof String pathPattern && !pathPattern.isBlank()) {
            return pathPattern;
        }
        return StrUtil.trimToNull(request.getRequestURI());
    }

    private String resolveRequestUri(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String requestUri = StrUtil.trimToNull(request.getRequestURI());
        String queryString = StrUtil.trimToNull(request.getQueryString());
        return requestUri == null || queryString == null ? requestUri : requestUri + "?" + queryString;
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        return principal == null ? null : principal.userId();
    }

    private String currentUsername() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        return principal == null ? null : principal.username();
    }

    private String currentUserType() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        return principal == null || principal.userType() == null ? null : principal.userType().name();
    }

    private String serializePermissionCodes() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        if (principal == null || principal.permissionCodes().isEmpty()) {
            return null;
        }
        Set<String> permissionCodes = principal.permissionCodes();
        return Json.toStr(permissionCodes.stream().sorted(Comparator.naturalOrder()).toList());
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof BizException bizException && bizException.getErrorCode() != null) {
            return bizException.getCode();
        }
        if (throwable instanceof SysException sysException && sysException.getOutwardCode() != null) {
            return sysException.getCode();
        }
        return BaseError.INTERNAL_ERROR.getCode();
    }
}
