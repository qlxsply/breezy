package com.corwin.framework.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for message-related APIs, extending {@link BaseRequest} with message-specific fields.
 *
 * @author Corwin 2026/1/8
 */
@Getter
@Setter
public class MessageRequest extends BaseRequest {
  /** Number of retry attempts for the message */
  private Integer retryCount;
}
