package com.corwin.system.notify.interfaces.web.res;

/**
 * Push 健康检查测试响应。
 *
 * @author Corwin 2026/3/21
 */
public record PushHealthTestRes(String message, PushHealthDeliveryRes delivery) {}
