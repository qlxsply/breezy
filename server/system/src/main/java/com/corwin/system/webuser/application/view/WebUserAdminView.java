package com.corwin.system.webuser.application.view;

import com.corwin.framework.constant.UserType;

import java.time.Instant;

/**
 * View for external user information displayed in the admin management panel.
 *
 * @param id          the user ID
 * @param account     the user account identifier
 * @param nickname    the display nickname
 * @param userType    the user type classification
 * @param status      the account status (e.g. ACTIVE, DISABLED, CANCELLED)
 * @param lastLoginAt timestamp of the most recent successful login
 * @param createdAt   timestamp when the user was created
 * @param updatedAt   timestamp when the user was last updated
 * @author Corwin 2026/5/11
 */
public record WebUserAdminView(
        Long id,
        String account,
        String nickname,
        UserType userType,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
