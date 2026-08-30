package com.corwin.system.notify.application.command;

/**
 * 保存 Push 订阅命令。
 *
 * @author Corwin 2026/3/19
 */
public record SavePushSubscriptionCommand(
    String deviceId, String endpoint, String p256dh, String auth) {}
