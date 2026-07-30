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
 * Request/response logging filter.
 *
 * <p>Core responsibilities:
 * <ul>
 *   <li>Log request entry info (protocol, method, path, client IP)</li>
 *   <li>Log request parameters and JSON body</li>
 *   <li>Log JSON response body (truncated to a maximum length)</li>
 *   <li>Log request duration and HTTP status code</li>
 * </ul>
 *
 * <p>Design constraints:
 * <ul>
 *   <li>Exclusion and streaming-path rules are driven by configuration, not hardcoded</li>
 *   <li>Streaming responses (e.g. SSE) skip the response wrapper to preserve semantics</li>
 *   <li>Body log size is capped at {@link #MAX_BODY_LOG_LENGTH} to prevent flooding</li>
 * </ul>
 *
 * <p>Related configuration:
 * <ul>
 *   <li>{@link DefaultConfigKeys#LOGGING_FILTER_EXCLUDE_PREFIXES} — path prefixes to skip</li>
 *   <li>{@link DefaultConfigKeys#LOGGING_FILTER_STREAM_PREFIXES} — path prefixes for streaming responses</li>
 * </ul>
 *
 * @author Corwin 2025/10/11
 */
@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter implements Ordered {

    /**
     * Maximum length for a single body log entry; longer content is truncated.
     */
    private static final int MAX_BODY_LOG_LENGTH = 4096;

    /**
     * Default fallback exclude prefixes when configuration cannot be read.
     */
    private static final List<String> DEFAULT_EXCLUDE_PREFIXES = List.of("/static/", "/actuator", "/favicon.ico");

    /**
     * Default fallback streaming path prefixes when configuration cannot be read.
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
     * Determines whether the request expects a streaming response.
     *
     * <p>Detection rules:
     * <ul>
     *   <li>The {@code Accept} header contains {@code text/event-stream}</li>
     *   <li>The request URI matches one of the configured stream path prefixes</li>
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
