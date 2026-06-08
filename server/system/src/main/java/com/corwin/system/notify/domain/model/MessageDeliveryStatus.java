package com.corwin.system.notify.domain.model;

/**
 * 消息投递状态。
 *
 * @author Corwin 2026/3/19
 */
public enum MessageDeliveryStatus {
    PENDING,
    SENT,
    ACKED,
    FAILED
}
