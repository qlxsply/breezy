package com.corwin.framework.web.ctx;

import com.corwin.framework.constant.MdcKeys;
import com.corwin.framework.web.auth.AuthPrincipal;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

/**
 * Request-scoped context object holding per-request state.
 *
 * <p>Stores tracing, authentication, client, and custom extension data associated with a single
 * HTTP request, carried across layers via {@link java.lang.ThreadLocal} (typically managed by
 * {@link CtxUtil}).
 *
 * <h2>Design goals</h2>
 *
 * <ul>
 *   <li>Centralise request-level data (tracing, user, client, custom attributes)
 *   <li>Avoid passing parameters through multiple method layers
 *   <li>Support log correlation via MDC injection
 *   <li>Support cross-thread propagation via {@link #copy()}
 * </ul>
 *
 * <h2>Thread model</h2>
 *
 * <ul>
 *   <li>Typically bound to a ThreadLocal — one instance per request thread
 *   <li>Not thread-safe; must not be shared across threads
 *   <li>Use {@link #copy()} before passing to another thread
 * </ul>
 *
 * <h2>Typical usage</h2>
 *
 * <ul>
 *   <li>Gateway / Filter initialises context at request entry
 *   <li>Authentication module writes user information
 *   <li>Logging system reads traceId / userId
 *   <li>Business code stores temporary contextual data
 * </ul>
 *
 * @author Corwin
 * @since 2026/3/30
 */
@Getter
@Setter
public class Ctx {

  /** Request start time in epoch millis. Used for logging and request duration calculation. */
  private long requestStartTs;

  /** Request start time in nanoseconds. Used for high-precision performance analysis. */
  private long requestStartNano;

  /** Distributed tracing ID, correlated across the entire call chain. */
  private String traceId;

  /** Current span ID within the distributed trace. */
  private String spanId;

  /** Parent span ID for the current span. */
  private String parentSpanId;

  /** Client identifier (e.g. web, app, service). */
  private String client;

  /** Client IP address. */
  private String clientIp;

  /** Authenticated principal for the current request. */
  private AuthPrincipal principal;

  /** Token hash for security auditing and log correlation. Plaintext token must never be stored. */
  private String tokenHash;

  /**
   * Extension attribute map for custom contextual data.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Lazy-initialized to avoid unnecessary memory overhead
   *   <li>Key-value structure with no type constraints (caller is responsible for type safety)
   * </ul>
   */
  private Map<String, Object> attributes;

  /**
   * Creates a shallow copy of the current context.
   *
   * <p>Used for propagating context across threads, e.g.:
   *
   * <ul>
   *   <li>Thread pool tasks
   *   <li>Async execution
   *   <li>CompletableFuture
   * </ul>
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>attributes is shallow-copied (Map copied, values remain references)
   *   <li>AuthPrincipal is immutable (record) and can be safely shared
   * </ul>
   *
   * @return a new Ctx instance with copied values
   */
  public Ctx copy() {
    Ctx copy = new Ctx();
    copy.requestStartTs = this.requestStartTs;
    copy.requestStartNano = this.requestStartNano;
    copy.traceId = this.traceId;
    copy.spanId = this.spanId;
    copy.parentSpanId = this.parentSpanId;
    copy.client = this.client;
    copy.clientIp = this.clientIp;
    copy.principal = this.principal;
    copy.tokenHash = this.tokenHash;
    if (this.attributes != null) {
      copy.attributes = new HashMap<>(this.attributes);
    }
    return copy;
  }

  /**
   * Binds context data to the SLF4J MDC (Mapped Diagnostic Context).
   *
   * <p>Enables log patterns to automatically include fields such as traceId, userId, and clientIp.
   *
   * <p>Calling convention:
   *
   * <ul>
   *   <li>At request entry (Filter / Interceptor)
   *   <li>After thread switch, to re-bind in the new thread
   * </ul>
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>MDC is ThreadLocal-based; re-binding is required after a thread switch
   *   <li>{@link #clearMdc()} must be called at request completion
   * </ul>
   */
  public void bindToMdc() {
    if (traceId != null) {
      MDC.put(MdcKeys.TRACE_ID, traceId);
    }
    if (spanId != null) {
      MDC.put(MdcKeys.SPAN_ID, spanId);
    }
    if (principal != null && principal.userId() != null) {
      MDC.put(MdcKeys.USER_ID, String.valueOf(principal.userId()));
    }
    if (principal != null && principal.username() != null) {
      MDC.put(MdcKeys.USER_NAME, principal.username());
    }
    if (clientIp != null) {
      MDC.put(MdcKeys.CLIENT_IP, clientIp);
    }
  }

  /**
   * Clears MDC context data at request completion.
   *
   * <p>Mandatory to prevent ThreadLocal leakage:
   *
   * <ul>
   *   <li>Log data cross-contamination on thread reuse
   *   <li>Unintended user information exposure
   * </ul>
   */
  public static void clearMdc() {
    MDC.remove(MdcKeys.TRACE_ID);
    MDC.remove(MdcKeys.SPAN_ID);
    MDC.remove(MdcKeys.USER_ID);
    MDC.remove(MdcKeys.USER_NAME);
    MDC.remove(MdcKeys.CLIENT_IP);
  }

  /**
   * Writes an extension attribute.
   *
   * @param key the attribute name
   * @param value the attribute value
   */
  public void putAttr(String key, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    attributes.put(key, value);
  }

  /**
   * Removes an extension attribute.
   *
   * <p>Sets the internal map to null when empty to reduce memory footprint.
   */
  public void removeAttr(String key) {
    if (attributes == null) {
      return;
    }
    attributes.remove(key);
    if (attributes.isEmpty()) {
      attributes = null;
    }
  }

  /** Returns an extension attribute without type checking. */
  public Object getAttr(String key) {
    return attributes == null ? null : attributes.get(key);
  }

  /**
   * Returns an extension attribute with type checking.
   *
   * @param key the attribute name
   * @param type the expected type
   * @return the attribute value, or null if absent
   * @throws ClassCastException if the value is not of the expected type
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttr(String key, Class<T> type) {
    if (attributes == null) {
      return null;
    }
    Object value = attributes.get(key);
    if (value == null) {
      return null;
    }
    if (!type.isInstance(value)) {
      String name = type.getName();
      String vname = value.getClass().getName();
      String msg =
          String.format("Attribute [%s] is not of type %s, actual type: %s", key, name, vname);
      throw new ClassCastException(msg);
    }
    return (T) value;
  }
}
