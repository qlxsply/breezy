package com.corwin.framework.util;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import jakarta.servlet.http.HttpServletRequest;

/**
 *
 * @author Corwin 2026/3/30
 * 2025/12/9
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
     * 直接取 header, Cloudflare / True-Client-IP，这些 Header 通常只有一个值。
     */
    private static String getSingleHeaderIp(String value) {
        return isValid(value) ? value.trim() : null;
    }

    /**
     * 从可能含多个 IP 的 header（如 X-Forwarded-For）中取首位或末尾
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

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

}
