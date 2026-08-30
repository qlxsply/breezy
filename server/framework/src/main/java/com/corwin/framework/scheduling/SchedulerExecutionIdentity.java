package com.corwin.framework.scheduling;

import com.corwin.framework.constant.UserType;
import java.util.Objects;

/**
 * Identity used for scheduled task execution.
 *
 * <p>Represents the system-level principal under which scheduled tasks run. All fields are
 * non-null.
 *
 * @param userId the scheduler system user ID
 * @param username the scheduler system username
 * @param userType the user type (typically {@link
 *     com.corwin.framework.constant.UserType#SCHEDULER})
 * @author Corwin 2026/3/23
 */
public record SchedulerExecutionIdentity(Long userId, String username, UserType userType) {
  public SchedulerExecutionIdentity {
    Objects.requireNonNull(userId, "userId required");
    Objects.requireNonNull(username, "username required");
    Objects.requireNonNull(userType, "userType required");
  }
}
