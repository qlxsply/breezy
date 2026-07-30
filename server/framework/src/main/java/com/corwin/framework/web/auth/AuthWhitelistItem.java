package com.corwin.framework.web.auth;

/**
 * A single whitelist entry parsed from configuration.
 *
 * @param type    the matching strategy (see {@link WhitelistMatchType})
 * @param pattern the path pattern to match against
 * @author Corwin 2026/3/23
 */
public record AuthWhitelistItem(
        String type,
        String pattern
) {
}
