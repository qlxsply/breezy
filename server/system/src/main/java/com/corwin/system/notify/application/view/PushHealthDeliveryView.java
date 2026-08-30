package com.corwin.system.notify.application.view;

import java.time.Instant;

/**
 * Push 投递健康状态视图。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthDeliveryView(
    String id,
    String msgType,
    String priority,
    String status,
    String route,
    Instant createdAt,
    Instant sentAt,
    Instant ackedAt) {}
