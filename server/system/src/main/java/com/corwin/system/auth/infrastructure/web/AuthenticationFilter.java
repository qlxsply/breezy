package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.json.Json;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.auth.SseTicketService;
import com.corwin.framework.web.auth.TokenPayload;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.infrastructure.security.AuthPrincipalAuthenticator;
import com.corwin.system.auth.infrastructure.security.OpaqueTokenService;
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
import java.util.Optional;

/**
 * Servlet filter that authenticates each incoming request by resolving the
 * authorization header (JWT or opaque token) or SSE ticket, then sets the
 * {@link AuthPrincipal} into the request context.
 *
 * @author Corwin 2026/4/19
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private final AuthPrincipalAuthenticator authenticator;
    private final OpaqueTokenService opaqueTokenService;
    private final Optional<SseTicketService> sseTicketService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        CtxUtil.setPrincipal(AuthPrincipal.guest());
        CtxUtil.clearTokenHash();
        try {
            String authorization = request.getHeader(HttpHeaderNames.AUTHORIZATION);
            String rawToken = resolveToken(authorization);
            AuthPrincipal principal;
            if (!rawToken.isBlank()) {
                principal = isJwt(rawToken)
                        ? authenticator.authenticateExternalToken(rawToken)
                        : authenticator.authenticateInternalToken(rawToken);
                String tokenHash = opaqueTokenService.hash(rawToken);
                CtxUtil.setPrincipal(principal);
                CtxUtil.setTokenHash(tokenHash);
            } else {
                principal = resolveBySseTicket(request);
                if (principal != null) {
                    CtxUtil.setPrincipal(principal);
                }
            }
        } catch (RuntimeException ex) {
            writeError(response, resolveError(ex));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private AuthPrincipal resolveBySseTicket(HttpServletRequest request) {
        if (sseTicketService.isEmpty()) {
            return null;
        }
        SseTicketService service = sseTicketService.get();
        if (!service.supports(request)) {
            return null;
        }
        String ticket = request.getParameter("sseTicket");
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        TokenPayload payload = service.consume(ticket);
        return authenticator.authenticateSseTicket(payload);
    }

    private String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    private boolean isJwt(String rawToken) {
        return rawToken.indexOf('.') > 0 && rawToken.indexOf('.') != rawToken.lastIndexOf('.');
    }

    private AuthError resolveError(RuntimeException ex) {
        if (ex instanceof com.corwin.framework.error.BizException bizException && bizException.getErrorCode() instanceof AuthError authError) {
            return authError;
        }
        return AuthError.INVALID_TOKEN;
    }

    private void writeError(HttpServletResponse response, AuthError error) throws IOException {
        int status = switch (error) {
            case FORBIDDEN, USER_DISABLED -> HttpServletResponse.SC_FORBIDDEN;
            default -> HttpServletResponse.SC_UNAUTHORIZED;
        };
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(Json.toStr(ApiResponse.fail(error)));
    }

    @Override
    public int getOrder() {
        // Run after other high-priority filters
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
