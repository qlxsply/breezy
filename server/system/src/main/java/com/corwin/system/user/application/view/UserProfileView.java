package com.corwin.system.user.application.view;

import com.corwin.framework.constant.UserType;
import java.time.Instant;

/**
 * View object representing an end-user's profile information.
 *
 * @author Corwin 2026/4/19
 */
public record UserProfileView(
    Long id,
    String username,
    String nickname,
    UserType userType,
    String status,
    boolean mustChangePassword,
    Instant createdAt,
    Instant updatedAt) {}
