package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.HighDate;
import com.corwin.system.auth.application.error.AuthError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

/**
 * @author Corwin 2026/8/20
 */
@Component
@RequiredArgsConstructor
public class AdminCsrfFilter extends OncePerRequestFilter implements Ordered {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> PUBLIC_PROTECTED_PATHS = Set.of(
            "/api/admin/auth/login",
            "/api/admin/auth/logout"
    );

    private final AdminAuthCookieService cookieService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod()) || !requiresProtection(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = cookieService.readCsrf(request);
        String headerToken = request.getHeader(AdminAuthCookieService.CSRF_HEADER);
        if (!matches(cookieToken, headerToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(Json.toStr(new CsrfErrorResponse(false, AuthError.FORBIDDEN.getCode(),
                    AuthError.FORBIDDEN.getMsg(), HighDate.realInstant(), null)));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresProtection(HttpServletRequest request) {
        if (PUBLIC_PROTECTED_PATHS.contains(request.getRequestURI())) {
            return true;
        }
        String authorization = request.getHeader(HttpHeaderNames.AUTHORIZATION);
        return (authorization == null || authorization.isBlank()) && !cookieService.readSession(request).isBlank();
    }

    private boolean matches(String cookieToken, String headerToken) {
        if (cookieToken == null || cookieToken.isBlank() || headerToken == null || headerToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.trim().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private record CsrfErrorResponse(boolean success, String code, String msg, Instant timestamp, Object data) {
    }
}
