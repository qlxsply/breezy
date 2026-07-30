package com.corwin.system.user.application.command;

import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;

/**
 * Command to batch update the status of multiple users.
 *
 * @author Corwin 2026/7/7
 */
public record BatchUpdateUserStatusCommand(
        List<Long> userIds,
        UserStatus status
) {
}
