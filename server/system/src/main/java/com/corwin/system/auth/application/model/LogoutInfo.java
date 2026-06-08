package com.corwin.system.auth.application.model;

/**
 * 登出解析结果。
 *
 * @author Corwin 2026/4/15
 */
public record LogoutInfo(
        Long userId,
        String account,
        boolean success,
        String message
) {

    public static LogoutInfo success(Long userId, String account) {
        return new LogoutInfo(userId, account, true, null);
    }

    public static LogoutInfo failure(Long userId, String account, String message) {
        return new LogoutInfo(userId, account, false, message);
    }
}
