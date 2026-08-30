package com.corwin.system.user.application.view;

import com.corwin.framework.constant.UserType;
import java.time.Instant;
import java.util.List;

/**
 * View object representing an admin user's profile, including recent login activities.
 *
 * @author Corwin 2026/6/4
 */
public record AdminProfileView(
    Long id,
    String username,
    String nickname,
    UserType userType,
    String status,
    boolean mustChangePassword,
    Instant lastPasswordChangedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AdminProfileLoginActivityView> recentActivities) {}
