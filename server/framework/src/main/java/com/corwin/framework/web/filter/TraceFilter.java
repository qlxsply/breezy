package com.corwin.framework.web.filter;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.ctx.TraceGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Trace filter — entry point for distributed tracing.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate or propagate traceId from upstream headers</li>
 *   <li>Generate spanId for the current node</li>
 *   <li>Initialise the request context ({@link com.corwin.framework.web.ctx.Ctx})</li>
 *   <li>Bind context data to MDC for log correlation</li>
 *   <li>Write traceId back in the response header</li>
 *   <li>Clean up the context after request completion</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 */
@Slf4j
@Component
public class TraceFilter extends OncePerRequestFilter implements Ordered {

    private static final int TRACE_ID_MAX_LENGTH = 64;
    private static final int SPAN_ID_MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // ===== 1. traceId: prefer propagation from upstream =====
        String traceId = resolveTraceId(request.getHeader(HttpHeaderNames.TRACE_ID));

        // ===== 2. spanId: generate for the current node =====
        String spanId = TraceGenerator.newSpanId();

        // ===== 3. parentSpanId: optional, from upstream =====
        String parentSpanId = resolveParentSpanId(request.getHeader(HttpHeaderNames.SPAN_ID));

        try {
            // ===== 4. initialize request context =====
            CtxUtil.initTrace(request, traceId);
            CtxUtil.setSpanInfo(spanId, parentSpanId);

            // ===== 5. bind to MDC for logging =====
            CtxUtil.bindMdc();

            // ===== 6. write trace ID back to response header =====
            response.setHeader(HttpHeaderNames.TRACE_ID, traceId);

            // ===== 7. proceed with the filter chain =====
            filterChain.doFilter(request, response);

        } finally {
            // ===== 8. guard: some containers may skip header writing =====
            if (!response.isCommitted()) {
                response.setHeader(HttpHeaderNames.TRACE_ID, traceId);
            }

            // ===== 9. mandatory context cleanup =====
            CtxUtil.reset();
        }
    }

    @Override
    public int getOrder() {
        return FilterOrder.TRACE_FILTER.value();
    }

    private String resolveTraceId(String rawHeaderTraceId) {
        String normalized = normalizeHeaderId(rawHeaderTraceId, TRACE_ID_MAX_LENGTH);
        if (normalized != null) {
            return normalized;
        }
        if (rawHeaderTraceId != null && !rawHeaderTraceId.isBlank()) {
            log.warn("Ignore invalid {} header: {}", HttpHeaderNames.TRACE_ID, abbreviate(rawHeaderTraceId));
        }
        return TraceGenerator.newTraceId();
    }

    private String resolveParentSpanId(String rawHeaderSpanId) {
        String normalized = normalizeHeaderId(rawHeaderSpanId, SPAN_ID_MAX_LENGTH);
        if (normalized != null) {
            return normalized;
        }
        if (rawHeaderSpanId != null && !rawHeaderSpanId.isBlank()) {
            log.warn("Ignore invalid {} header: {}", HttpHeaderNames.SPAN_ID, abbreviate(rawHeaderSpanId));
        }
        return null;
    }

    private String normalizeHeaderId(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > maxLength) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean digit = ch >= '0' && ch <= '9';
            boolean lower = ch >= 'a' && ch <= 'z';
            boolean upper = ch >= 'A' && ch <= 'Z';
            if (!digit && !lower && !upper && ch != '-' && ch != '_') {
                return null;
            }
        }
        return value;
    }

    private String abbreviate(String raw) {
        String value = raw == null ? "" : raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (value.length() <= 80) {
            return value;
        }
        return value.substring(0, 80) + "...";
    }

}
