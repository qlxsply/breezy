package com.corwin.system.notify.interfaces.web.res;

/**
 * SSE 短时票据返回对象。
 *
 * @author Corwin 2026/3/19
 */
public record SseTicketRes(
        String ticket,
        long expiresAtEpochMillis
) {
}
