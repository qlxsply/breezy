package com.corwin.system.diagnostic.infrastructure.web;

import com.corwin.framework.util.HighDate;
import com.corwin.system.diagnostic.domain.model.DiagnosticItem;
import com.corwin.system.diagnostic.infrastructure.collector.HttpRequestObserver;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class DiagnosticHttpFilter extends OncePerRequestFilter implements Ordered {

    private final DiagnosticRuntimeManager runtimeManager;
    private final HttpRequestObserver httpRequestObserver;

    public DiagnosticHttpFilter(DiagnosticRuntimeManager runtimeManager, HttpRequestObserver httpRequestObserver) {
        this.runtimeManager = runtimeManager;
        this.httpRequestObserver = httpRequestObserver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!runtimeManager.isCollecting(DiagnosticItem.HTTP)) {
            filterChain.doFilter(request, response);
            return;
        }
        long start = HighDate.realTimestampMillis();
        httpRequestObserver.onRequestStarted();
        Throwable error = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            error = ex;
            if (ex instanceof IOException ioEx) {
                throw ioEx;
            }
            if (ex instanceof ServletException servletEx) {
                throw servletEx;
            }
            throw new ServletException(ex);
        } finally {
            long durationMs = HighDate.realTimestampMillis() - start;
            httpRequestObserver.onRequestCompleted(request.getMethod(), request.getRequestURI(), response.getStatus(),
                    durationMs, error, runtimeManager.slowRequestThresholdMs());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
