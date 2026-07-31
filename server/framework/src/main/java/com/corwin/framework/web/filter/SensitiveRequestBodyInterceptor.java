package com.corwin.framework.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Corwin 2026/7/31
 */
final class SensitiveRequestBodyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method && isSensitive(method)) {
            request.setAttribute(SensitiveRequestBody.ATTRIBUTE, Boolean.TRUE);
        }
        return true;
    }

    private boolean isSensitive(HandlerMethod method) {
        return method.hasMethodAnnotation(SensitiveRequestBody.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), SensitiveRequestBody.class);
    }
}
