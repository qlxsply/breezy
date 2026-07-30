package com.corwin.system.config.interfaces.web.req;

/**
 * Request DTO for client IP resolution preview.
 * Accepts the desired client IP resolution mode to preview the resolved result.
 *
 * @author Corwin 2026/3/11
 */
public record ConfigClientIpPreviewReq(String mode) {
}
