package com.corwin.framework.event.durable.store;

/**
 * Delivery status for durable event delivery.
 *
 * @author Corwin 2026/4/12
 */
public enum DeliveryStatus {
  PENDING,
  CLAIMED,
  SUCCEEDED,
  CANCELLED
}
