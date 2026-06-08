package com.corwin.system.config.interfaces.web.res;

/**
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
