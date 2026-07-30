package com.corwin.framework.constant;

/**
 * MDC (Mapped Diagnostic Context) key constants for log correlation.
 * <p>
 * These keys are used to store tracing and user context in SLF4J MDC,
 * enabling log patterns to automatically include fields such as
 * traceId, userId, and clientIp.
 *
 * @author Corwin 2026/3/23
 */
public final class MdcKeys {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String USER_NAME = "username";
    public static final String USER_TYPE = "userType";
    public static final String CLIENT_IP = "clientIp";

    private MdcKeys() {
    }

}
