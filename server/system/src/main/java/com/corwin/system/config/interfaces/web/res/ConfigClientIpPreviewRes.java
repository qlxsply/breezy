package com.corwin.system.config.interfaces.web.res;

/**
 * Response DTO for client IP resolution preview.
 * Shows the resolved IP along with raw header values from various
 * proxy forwarding headers such as X-Real-IP, X-Forwarded-For, etc.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigClientIpPreviewRes(
        String mode,
        String resolvedIp,
        String remoteAddr,
        String xRealIp,
        String xForwardedFor,
        String cfConnectingIp,
        String trueClientIp
) {
}
