package com.corwin.system.config.application.view;

/**
 * View object for client IP resolution preview.
 * Contains the resolved IP along with all raw header values used during resolution.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigClientIpPreviewView(
        String mode,
        String resolvedIp,
        String remoteAddr,
        String xRealIp,
        String xForwardedFor,
        String cfConnectingIp,
        String trueClientIp
) {
}
