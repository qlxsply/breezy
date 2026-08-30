package com.corwin.system.notify.domain.model;

/**
 * Enumeration of message delivery states.
 *
 * <p>Represents the lifecycle of a message delivery: PENDING, SENT, ACKED, or FAILED.
 *
 * @author Corwin 2026/3/19
 */
public enum MessageDeliveryStatus {
  PENDING,
  SENT,
  ACKED,
  FAILED
}
