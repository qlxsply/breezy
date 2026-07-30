package com.corwin.system.auth.application.model;

/**
 * Result of a logout operation, indicating success or failure with details.
 *
 * @author Corwin 2026/4/15
 */
public record LogoutInfo(
        Long userId,
        String account,
        boolean success,
        String message
) {

    /**
     * Creates a successful logout info.
     */
    public static LogoutInfo success(Long userId, String account) {
        return new LogoutInfo(userId, account, true, null);
    }

    /**
     * Creates a failed logout info with a reason message.
     */
    public static LogoutInfo failure(Long userId, String account, String message) {
        return new LogoutInfo(userId, account, false, message);
    }
}
