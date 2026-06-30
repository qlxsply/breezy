package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.error.BizException;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.application.service.RequestAuthorizationService;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.auth.published.PermitAll;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author Corwin 2026/4/19
 */
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final RequestAuthorizationService authorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        PermitAll methodPermitAll = handlerMethod.getMethodAnnotation(PermitAll.class);
        if (methodPermitAll != null) {
            return true;
        }

        Authorize methodAuthorize = handlerMethod.getMethodAnnotation(Authorize.class);
        if (methodAuthorize != null) {
            authorizationService.checkAuthorized(methodAuthorize.userType(), methodAuthorize.permissions(),
                    methodAuthorize.anyPermission());
            return true;
        }

        Authenticated methodAuthenticated = handlerMethod.getMethodAnnotation(Authenticated.class);
        if (methodAuthenticated != null) {
            authorizationService.checkAuthenticated(methodAuthenticated.userType());
            return true;
        }

        PermitAll typePermitAll = handlerMethod.getBeanType().getAnnotation(PermitAll.class);
        if (typePermitAll != null) {
            return true;
        }

        Authorize typeAuthorize = handlerMethod.getBeanType().getAnnotation(Authorize.class);
        if (typeAuthorize != null) {
            authorizationService.checkAuthorized(typeAuthorize.userType(), typeAuthorize.permissions(),
                    typeAuthorize.anyPermission());
            return true;
        }

        Authenticated typeAuthenticated = handlerMethod.getBeanType().getAnnotation(Authenticated.class);
        if (typeAuthenticated != null) {
            authorizationService.checkAuthenticated(typeAuthenticated.userType());
            return true;
        }

        throw new BizException(AuthError.PERMISSION_DECLARATION_MISSING);
    }
}
