package com.corwin.framework.constant;

/**
 * HTTP 请求头常量。
 *
 * @author Corwin 2026/3/23
 */
public final class HttpHeaderNames {

    public static final String TRACE_ID = "X-Trace-Id";
    public static final String SPAN_ID = "X-Span-Id";
    public static final String CLIENT = "X-Client";
    public static final String CLIENT_ID = "X-Client-Id";
    public static final String AUTHORIZATION = "Authorization";

    private HttpHeaderNames() {
    }
}
