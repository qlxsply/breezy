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
 * Trace 过滤器（请求入口）。
 *
 * <p>职责：
 * <ul>
 *     <li>生成 / 透传 traceId</li>
 *     <li>生成 spanId</li>
 *     <li>初始化 Ctx</li>
 *     <li>绑定 MDC</li>
 *     <li>在响应头中写回 traceId</li>
 *     <li>请求结束后清理上下文</li>
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

        // ===== 1. traceId：优先透传 =====
        String traceId = resolveTraceId(request.getHeader(HttpHeaderNames.TRACE_ID));

        // ===== 2. spanId：当前节点 =====
        String spanId = TraceGenerator.newSpanId();

        // ===== 3. parentSpanId：来自上游（可选）=====
        String parentSpanId = resolveParentSpanId(request.getHeader(HttpHeaderNames.SPAN_ID));

        try {
            // ===== 4. 初始化上下文 =====
            CtxUtil.initTrace(request, traceId);
            CtxUtil.setSpanInfo(spanId, parentSpanId);

            // ===== 5. 绑定 MDC（统一入口）=====
            CtxUtil.bindMdc();

            // ===== 6. 回写响应头（用于链路透传）=====
            response.setHeader(HttpHeaderNames.TRACE_ID, traceId);

            // ===== 7. 继续执行 =====
            filterChain.doFilter(request, response);

        } finally {
            // ===== 8. 防止部分容器未写 header（极端情况）=====
            if (!response.isCommitted()) {
                response.setHeader(HttpHeaderNames.TRACE_ID, traceId);
            }

            // ===== 9. 清理上下文（必须）=====
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
