package com.corwin.framework.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Base request DTO carrying common metadata fields.
 *
 * <p>Fields such as API version, timestamp, client ID, and idempotency key are shared across many
 * API requests.
 *
 * @author Corwin 2026/1/8
 */
@Getter
@Setter
public class BaseRequest {
  /** API version identifier for backward compatibility */
  private String apiVersion;

  /** Client-side timestamp (epoch millis) for request freshness checks */
  private Long timestamp;

  /** Origin system identifier, commonly used for external system integration */
  private String clientId;

  /** Idempotency key for safe retry of write operations */
  private String idemKey;
}
