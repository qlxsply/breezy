package com.corwin.framework.web.filter;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请求/响应日志过滤器。
 *
 * <p>核心职责：
 * <ul>
 *     <li>记录请求入口信息（协议、方法、路径、来源IP）</li>
 *     <li>记录请求参数与 JSON 请求体</li>
 *     <li>记录 JSON 响应体（受长度限制）</li>
 *     <li>记录请求耗时与状态码</li>
 * </ul>
 *
 * <p>设计约束：
 * <ul>
 *     <li>路径排除规则与流式路径规则由配置中心驱动，不在框架层硬编码业务路径</li>
 *     <li>对于流式响应（如 SSE），跳过响应包装器，避免破坏持续输出语义</li>
 *     <li>日志体积受 {@link #MAX_BODY_LOG_LENGTH} 控制，避免大报文刷屏</li>
 * </ul>
 *
 * <p>相关配置：
 * <ul>
 *     <li>{@link DefaultConfigKeys#LOGGING_FILTER_EXCLUDE_PREFIXES}：过滤器跳过路径前缀</li>
 *     <li>{@link DefaultConfigKeys#LOGGING_FILTER_STREAM_PREFIXES}：按路径识别流式响应（同时保留 Accept 识别）</li>
 * </ul>
 *
 * @author Corwin 2025/10/11
 */
@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter implements Ordered {

    /**
     * 单次日志输出体的最大长度，超长截断。
     */
    private static final int MAX_BODY_LOG_LENGTH = 4096;

    /**
     * 配置读取失败时的兜底排除前缀。
     */
    private static final List<String> DEFAULT_EXCLUDE_PREFIXES = List.of("/static/", "/actuator", "/favicon.ico");

    /**
     * 配置读取失败时的兜底流式路径前缀。
     */
    private static final List<String> DEFAULT_STREAM_PREFIXES = List.of("/api/sse/");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = HighDate.realTimestampMillis();

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String protocol = request.getProtocol();
        String remote = IpUtil.getClientIp(request);

        ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(request);
        boolean streamRequest = isStreamRequest(request);
        ContentCachingResponseWrapper wrappedRes = streamRequest ? null : new ContentCachingResponseWrapper(response);
        HttpServletResponse responseToUse = wrappedRes == null ? response : wrappedRes;

        log.info("--> {} {} {} from {}", protocol, method, uri, remote);

        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            log.info("--> params {}", formatParams(params));
        }

        Throwable error = null;
        try {
            filterChain.doFilter(wrappedReq, responseToUse);
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
            logRequestBody(wrappedReq);

            if (wrappedRes != null) {
                logResponseBody(wrappedRes);
                wrappedRes.copyBodyToResponse();
            }

            long cost = HighDate.realTimestampMillis() - start;
            int status = response.getStatus();

            if (streamRequest) {
                if (error == null) {
                    log.info("<-- {} {} [stream] {}ms", method, uri, cost);
                } else {
                    log.warn("<-- {} {} [stream] failed {}ms {}", method, uri, cost, error.getClass().getSimpleName());
                }
            } else {
                if (error == null) {
                    log.info("<-- {} {} {} {}ms", method, uri, status, cost);
                } else {
                    log.warn("<-- {} {} {} {}ms {}", method, uri, status, cost, error.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * 判定是否为流式响应请求。
     *
     * <p>判定规则：
     * <ul>
     *     <li>请求头 Accept 包含 text/event-stream</li>
     *     <li>或命中配置项 {@link DefaultConfigKeys#LOGGING_FILTER_STREAM_PREFIXES} 的路径前缀</li>
     * </ul>
     */
    private boolean isStreamRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && !accept.isBlank()) {
            String normalizedAccept = accept.toLowerCase();
            if (normalizedAccept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
                return true;
            }
        }

        String uri = request.getRequestURI();
        return resolvePrefixes(DefaultConfigKeys.LOGGING_FILTER_STREAM_PREFIXES, DEFAULT_STREAM_PREFIXES).stream().anyMatch(
                uri::startsWith);
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        if (isJsonContentType(request.getContentType())) {
            logBody("--> json ", request.getContentAsByteArray());
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response) {
        if (isJsonContentType(response.getContentType())) {
            logBody("<-- json ", response.getContentAsByteArray());
        }
    }

    private boolean isJsonContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.toLowerCase();
        return normalized.contains(MediaType.APPLICATION_JSON_VALUE) || normalized.contains("+json");
    }

    private void logBody(String prefix, byte[] body) {
        if (body == null || body.length == 0) {
            return;
        }
        int len = Math.min(body.length, MAX_BODY_LOG_LENGTH);
        String text = new String(body, 0, len, StandardCharsets.UTF_8);
        if (body.length > MAX_BODY_LOG_LENGTH) {
            text += "...(truncated, total=" + body.length + ")";
        }
        log.info("{}{}", prefix, text);
    }

    private String formatParams(Map<String, String[]> params) {
        return params.entrySet().stream().map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                     .collect(Collectors.joining(", ", "{", "}"));
    }

    private List<String> resolvePrefixes(DefaultConfigKeys configKey, List<String> fallback) {
        try {
            List<String> values = ConfigRegistry.strListV(configKey);
            if (values == null || values.isEmpty()) {
                return fallback;
            }
            return values.stream().map(it -> it == null ? "" : it.trim()).filter(it -> !it.isBlank())
                         .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Resolve logging filter prefixes failed, fallback to defaults: key={}", configKey.name(), ex);
            return fallback;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return resolvePrefixes(DefaultConfigKeys.LOGGING_FILTER_EXCLUDE_PREFIXES, DEFAULT_EXCLUDE_PREFIXES).stream().anyMatch(
                uri::startsWith);
    }

    @Override
    public int getOrder() {
        return FilterOrder.LOGGING_FILTER.value();
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
