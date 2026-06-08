package com.corwin.framework.event.durable.store;

/**
 * durable 投递状态。
 *
 * @author Corwin 2026/4/12
 */
public enum DeliveryStatus {
    PENDING,
    CLAIMED,
    SUCCEEDED,
    CANCELLED
}

