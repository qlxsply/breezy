package com.corwin.framework.web.auth;

/**
 * @author Corwin 2026/3/23
 */
public record AuthWhitelistItem(
        String type,
        String pattern
) {
}
