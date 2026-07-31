package com.corwin.framework.web.filter;

import com.corwin.framework.config.builtin.FrameworkConfigSpecs;
import com.corwin.framework.config.runtime.Configs;
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
 * @author Corwin 2025/10/11
 */
@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final int MAX_BODY_LOG_LENGTH = 4096;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long start = HighDate.realTimestampMillis();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String protocol = request.getProtocol();
        String remote = IpUtil.getClientIp(request);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        boolean streamRequest = isStreamRequest(request);
        ContentCachingResponseWrapper wrappedResponse = streamRequest
                ? null
                : new ContentCachingResponseWrapper(response);
        HttpServletResponse responseToUse = wrappedResponse == null ? response : wrappedResponse;

        log.info("--> {} {} {} from {}", protocol, method, uri, remote);
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            log.info("--> params {}", formatParams(params));
        }

        Throwable error = null;
        try {
            filterChain.doFilter(wrappedRequest, responseToUse);
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
            logRequestBody(wrappedRequest);
            if (wrappedResponse != null) {
                logResponseBody(wrappedResponse);
                wrappedResponse.copyBodyToResponse();
            }

            long cost = HighDate.realTimestampMillis() - start;
            int status = response.getStatus();
            if (streamRequest) {
                logStreamCompletion(method, uri, cost, error);
            } else {
                logCompletion(method, uri, status, cost, error);
            }
        }
    }

    private boolean isStreamRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && !accept.isBlank()
                && accept.toLowerCase().contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return true;
        }
        String uri = request.getRequestURI();
        return Configs.get(FrameworkConfigSpecs.LOGGING_FILTER).streamPrefixes().stream()
                .anyMatch(uri::startsWith);
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        if (Boolean.TRUE.equals(request.getAttribute(SensitiveRequestBody.ATTRIBUTE))) {
            return;
        }
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
        int length = Math.min(body.length, MAX_BODY_LOG_LENGTH);
        String text = new String(body, 0, length, StandardCharsets.UTF_8);
        if (body.length > MAX_BODY_LOG_LENGTH) {
            text += "...(truncated, total=" + body.length + ")";
        }
        log.info("{}{}", prefix, text);
    }

    private String formatParams(Map<String, String[]> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private void logStreamCompletion(String method, String uri, long cost, Throwable error) {
        if (error == null) {
            log.info("<-- {} {} [stream] {}ms", method, uri, cost);
        } else {
            log.warn("<-- {} {} [stream] failed {}ms {}", method, uri, cost, error.getClass().getSimpleName());
        }
    }

    private void logCompletion(String method, String uri, int status, long cost, Throwable error) {
        if (error == null) {
            log.info("<-- {} {} {} {}ms", method, uri, status, cost);
        } else {
            log.warn("<-- {} {} {} {}ms {}", method, uri, status, cost, error.getClass().getSimpleName());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return Configs.get(FrameworkConfigSpecs.LOGGING_FILTER).excludePrefixes().stream()
                .anyMatch(uri::startsWith);
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
