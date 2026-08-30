package com.corwin.framework.constant;

/**
 * Standard HTTP header name constants used by the framework.
 *
 * <p>Includes custom headers for distributed tracing (X-Trace-Id, X-Span-Id), client identification
 * (X-Client), and standard auth header (Authorization).
 *
 * @author Corwin 2026/3/23
 */
public final class HttpHeaderNames {

  public static final String TRACE_ID = "X-Trace-Id";
  public static final String SPAN_ID = "X-Span-Id";
  public static final String CLIENT = "X-Client";
  public static final String CLIENT_ID = "X-Client-Id";
  public static final String AUTHORIZATION = "Authorization";

  private HttpHeaderNames() {}
}
