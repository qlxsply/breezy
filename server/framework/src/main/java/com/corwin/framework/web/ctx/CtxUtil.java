package com.corwin.framework.web.ctx;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.IpUtil;
import com.corwin.framework.web.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Request context utility for managing the current thread-bound {@link Ctx}.
 * <p>
 * Based on {@link ThreadLocal}, it stores and provides access to request-scoped
 * data throughout a single request processing lifecycle.
 *
 * <h2>Core responsibilities</h2>
 * <ul>
 *   <li>Initialise request context (trace, client, start time, etc.)</li>
 *   <li>Provide a central read/write entry point for contextual data</li>
 *   <li>Support context snapshot and restore for async thread propagation</li>
 *   <li>Bind context to MDC for distributed log tracing</li>
 * </ul>
 *
 * <h2>Thread model</h2>
 * <ul>
 *   <li>Each thread holds its own {@link Ctx} instance</li>
 *   <li>Context does not propagate across threads by default</li>
 *   <li>For async execution, call {@link #snapshot()} first, then {@link #restore(Ctx)} in the target thread</li>
 * </ul>
 *
 * <h2>Lifecycle recommendation</h2>
 * <ol>
 *   <li>Call {@link #initTrace(HttpServletRequest, String)} at request entry</li>
 *   <li>Call {@link #setPrincipal(AuthPrincipal)} after authentication</li>
 *   <li>Call {@link #bindMdc()} before logging</li>
 *   <li>Call {@link #reset()} at request completion — mandatory</li>
 * </ol>
 *
 * <h2>Important notes</h2>
 * <ul>
 *   <li>This class relies on ThreadLocal; correct reset is essential in thread-pool scenarios</li>
 *   <li>Do not pass the current thread's Ctx directly to another thread; use a snapshot copy</li>
 *   <li>This is a static utility class and must not be instantiated</li>
 * </ul>
 *
 * @author Corwin
 * @since 2025/10/13
 */
public final class CtxUtil {

    /**
     * Current thread-bound request context cache.
     *
     * <p>Lazily initialised via {@link ThreadLocal#withInitial(java.util.function.Supplier)},
     * ensuring a usable {@link Ctx} instance is available on first access.</p>
     */
    private static final ThreadLocal<Ctx> CTX_CACHE = ThreadLocal.withInitial(Ctx::new);

    /**
     * Utility class — no instantiation allowed.
     */
    private CtxUtil() {
    }

    /**
     * Returns the context bound to the current thread.
     *
     * <p>If the current thread has not been initialised yet, a new empty
     * {@link Ctx} instance is created automatically.</p>
     *
     * @return the current thread's context
     */
    public static Ctx get() {
        return CTX_CACHE.get();
    }

    /**
     * Returns a snapshot copy of the current context.
     *
     * <p>Primarily used for context propagation in async tasks, thread pools,
     * and concurrent execution. Returns a copy rather than the original to
     * avoid sharing the same instance across threads.</p>
     *
     * @return a copy of the current context
     */
    public static Ctx snapshot() {
        return CTX_CACHE.get().copy();
    }

    /**
     * Restores the given context to the current thread.
     *
     * <p>Typically used before executing an async task to restore the parent
     * thread's context. A copy is saved internally to prevent external mutation.</p>
     *
     * <p>If {@code ctx} is null, this is equivalent to calling {@link #reset()}.</p>
     *
     * @param ctx the context to restore
     */
    public static void restore(Ctx ctx) {
        if (ctx == null) {
            reset();
            return;
        }
        CTX_CACHE.set(ctx.copy());
    }

    /**
     * Initialises the basic context for an HTTP request.
     *
     * <p>Initialisation includes:</p>
     * <ul>
     *   <li>Request start time (epoch millis)</li>
     *   <li>Request start time (nanos)</li>
     *   <li>traceId</li>
     *   <li>Client identifier (from request header)</li>
     *   <li>Client IP</li>
     * </ul>
     *
     * <p>Typically invoked in a Filter, Interceptor, or gateway entry point.</p>
     *
     * @param request the current HTTP request
     * @param traceId the trace ID assigned to this request
     */
    public static void initTrace(HttpServletRequest request, String traceId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setRequestStartTs(HighDate.mockTimestampMillis());
        ctx.setRequestStartNano(System.nanoTime());
        ctx.setTraceId(traceId);
        ctx.setClient(request.getHeader(HttpHeaderNames.CLIENT));
        ctx.setClientIp(IpUtil.getClientIp(request));
    }

    /**
     * Initialises context for scheduled / background tasks (non-HTTP threads).
     *
     * <p>Intended for:</p>
     * <ul>
     *   <li>Scheduled tasks</li>
     *   <li>Background jobs</li>
     *   <li>Message consumers</li>
     * </ul>
     *
     * <p>Since there is no HTTP request, client and clientIp are left null.</p>
     *
     * @param traceId the trace ID for the current task
     */
    public static void schedulerInitTrace(String traceId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setRequestStartTs(HighDate.mockTimestampMillis());
        ctx.setRequestStartNano(System.nanoTime());
        ctx.setTraceId(traceId);
        ctx.setClient(null);
        ctx.setClientIp(null);
    }

    /**
     * Sets the span information for the current thread.
     *
     * @param spanId       the current span ID
     * @param parentSpanId the parent span ID
     */
    public static void setSpanInfo(String spanId, String parentSpanId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setSpanId(spanId);
        ctx.setParentSpanId(parentSpanId);
    }

    /**
     * Sets the trace ID.
     *
     * @param traceId the distributed tracing ID
     */
    public static void setTraceId(String traceId) {
        CTX_CACHE.get().setTraceId(traceId);
    }

    /**
     * Sets the span ID.
     *
     * @param spanId the current call node ID
     */
    public static void setSpanId(String spanId) {
        CTX_CACHE.get().setSpanId(spanId);
    }

    /**
     * Sets the authenticated principal for the current request.
     *
     * <p>Typically called after successful authentication.</p>
     *
     * @param principal the current user identity
     */
    public static void setPrincipal(AuthPrincipal principal) {
        CTX_CACHE.get().setPrincipal(principal);
    }

    /**
     * Returns the current authenticated principal.
     *
     * @return the current user identity, or null if not authenticated
     */
    public static AuthPrincipal getPrincipal() {
        return CTX_CACHE.get().getPrincipal();
    }

    /**
     * Sets the token hash value.
     *
     * <p>Used for recording a digest of the current authentication token
     * for auditing, tracking, or secondary validation.
     * The plaintext token must never be stored.</p>
     *
     * @param tokenHash the token digest
     */
    public static void setTokenHash(String tokenHash) {
        CTX_CACHE.get().setTokenHash(tokenHash);
    }

    /**
     * Returns the current token hash.
     *
     * @return the token digest
     */
    public static String getTokenHash() {
        return CTX_CACHE.get().getTokenHash();
    }

    /**
     * Clears the current token hash.
     */
    public static void clearTokenHash() {
        CTX_CACHE.get().setTokenHash(null);
    }

    /**
     * Writes an extension attribute.
     *
     * <p>Used to store additional contextual data for the current request,
     * such as business identifiers, extended security info, or feature flags.</p>
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public static void putAttr(String key, Object value) {
        CTX_CACHE.get().putAttr(key, value);
    }

    /**
     * Removes an extension attribute.
     *
     * @param key the attribute key
     */
    public static void removeAttr(String key) {
        CTX_CACHE.get().removeAttr(key);
    }

    /**
     * Returns an extension attribute with type checking.
     *
     * <p>Returns null if the attribute is absent.
     * Throws {@link ClassCastException} if the value is not of the expected type.</p>
     *
     * @param key  the attribute key
     * @param type the expected type
     * @param <T>  the type parameter
     * @return the attribute value, or null if absent
     */
    public static <T> T getAttr(String key, Class<T> type) {
        return CTX_CACHE.get().getAttr(key, type);
    }

    /**
     * Binds the current context data to MDC.
     *
     * <p>Call this at request entry or after restoring context in an async
     * thread so that log patterns automatically include traceId, userId,
     * clientIp, etc.</p>
     */
    public static void bindMdc() {
        CTX_CACHE.get().bindToMdc();
    }

    /**
     * Clears context fields from MDC.
     *
     * <p>Typically called at request completion to prevent log
     * cross-contamination on thread reuse.</p>
     */
    public static void clearMdc() {
        Ctx.clearMdc();
    }

    /**
     * Resets the current thread's context.
     *
     * <p>This operation:</p>
     * <ul>
     *   <li>Clears MDC</li>
     *   <li>Removes the ThreadLocal context for the current thread</li>
     * </ul>
     *
     * <p>This is a mandatory cleanup at the end of every request.
     * Without it, thread-pool reuse could leak data from one request
     * to the next.</p>
     */
    public static void reset() {
        clearMdc();
        CTX_CACHE.remove();
    }

    /**
     * Returns the request start time in epoch millis.
     *
     * @return the request start timestamp
     */
    public static long getRequestStartTs() {
        return CTX_CACHE.get().getRequestStartTs();
    }

    /**
     * Returns the request start time in nanoseconds.
     *
     * @return the request start nanos
     */
    public static long getRequestStartNano() {
        return CTX_CACHE.get().getRequestStartNano();
    }

    /**
     * Returns the current trace ID.
     *
     * @return the trace ID
     */
    public static String getTraceId() {
        return CTX_CACHE.get().getTraceId();
    }

    /**
     * Returns the current trace ID, throwing if absent.
     *
     * <p>Useful when the current thread is required to have completed
     * trace initialisation.</p>
     *
     * @return the non-null trace ID
     * @throws IllegalStateException if no trace ID has been set for this thread
     */
    public static String getRequiredTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalStateException("Current thread traceId is missing");
        }
        return traceId;
    }

    /**
     * Returns the current span ID.
     *
     * @return the span ID
     */
    public static String getSpanId() {
        return CTX_CACHE.get().getSpanId();
    }

    /**
     * Returns the parent span ID.
     *
     * @return the parent span ID
     */
    public static String getParentSpanId() {
        return CTX_CACHE.get().getParentSpanId();
    }

    /**
     * Returns the client identifier.
     *
     * @return the client identifier
     */
    public static String getClient() {
        return CTX_CACHE.get().getClient();
    }

    /**
     * Returns the client IP address.
     *
     * @return the client IP
     */
    public static String getClientIp() {
        return CTX_CACHE.get().getClientIp();
    }

}
