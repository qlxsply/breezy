package com.corwin.framework.util;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the real client IP address from an HTTP request.
 * <p>
 * Supports multiple proxy header modes (X-Real-IP, X-Forwarded-For,
 * CF-Connecting-IP, True-Client-IP) configurable via
 * {@link com.corwin.framework.config.DefaultConfigKeys#CLIENT_IP_MODE}.
 *
 * @author Corwin 2026/3/30
 */
public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        String mode = ConfigRegistry.stringV(DefaultConfigKeys.CLIENT_IP_MODE);
        return resolveClientIp(ClientIpMode.valueOf(mode),
                request.getRemoteAddr(),
                request.getHeader("X-Real-IP"),
                request.getHeader("X-Forwarded-For"),
                request.getHeader("CF-Connecting-IP"),
                request.getHeader("True-Client-IP"));
    }

    public static String resolveClientIp(
            ClientIpMode mode,
            String remoteAddr,
            String xRealIp,
            String xForwardedFor,
            String cfConnectingIp,
            String trueClientIp) {
        return switch (mode) {
            case X_REAL_IP -> getSingleHeaderIp(xRealIp);
            case X_FORWARDED_FOR_FIRST -> getHeaderListIp(xForwardedFor, true);
            case X_FORWARDED_FOR_LAST -> getHeaderListIp(xForwardedFor, false);
            case CF_Connecting_IP -> getSingleHeaderIp(cfConnectingIp);
            case True_Client_IP -> getSingleHeaderIp(trueClientIp);
            default -> remoteAddr;
        };
    }

    /**
     * Returns the value directly from a single-value header
     * (e.g. Cloudflare / True-Client-IP).
     */
    private static String getSingleHeaderIp(String value) {
        return isValid(value) ? value.trim() : null;
    }

    /**
     * Returns the first or last IP from a multi-value header
     * (e.g. X-Forwarded-For).
     *
     * @param value the header value, may be null or blank
     * @param first if {@code true}, returns the first IP; otherwise the last
     * @return the extracted IP, or null if invalid
     */
    private static String getHeaderListIp(String value, boolean first) {
        if (!isValid(value)) {
            return null;
        }

        String[] parts = StrUtil.split(value);
        if (parts.length == 0) {
            return null;
        }

        String ip = first ? parts[0].trim() : parts[parts.length - 1].trim();
        return isValid(ip) ? ip : null;
    }

    /**
     * Checks whether the given IP string is non-null, non-blank, and
     * not the literal "unknown" (case-insensitive).
     *
     * @param ip the IP string to validate
     * @return {@code true} if the IP appears to be a real client address
     */
    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

}
