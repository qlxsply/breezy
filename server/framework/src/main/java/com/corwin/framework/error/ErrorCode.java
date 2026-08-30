package com.corwin.framework.error;

/**
 * Unified error-code contract for API responses.
 *
 * <p>The framework provides {@link BaseError}; business modules may implement this interface with
 * their own enums.
 *
 * @author Corwin 2026/3/30
 */
public interface ErrorCode {

  /** Business error code (distinct from HTTP status codes). */
  String getCode();

  /** Human-readable message for the API consumer. */
  String getMsg();

  /** The error-code range this code belongs to. */
  ErrorCodeRange getRange();
}
