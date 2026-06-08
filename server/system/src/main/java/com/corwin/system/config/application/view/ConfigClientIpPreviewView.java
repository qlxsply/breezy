package com.corwin.system.config.application.view;

/**
 * @author Corwin 2026/3/11
 */
public record ConfigClientIpPreviewView(
        String mode,
        String resolvedIp,
        String remoteAddr,
        String xRealIp,
        String xForwardedFor,
        String cfConnectingIp,
        String trueClientIp) {
}
