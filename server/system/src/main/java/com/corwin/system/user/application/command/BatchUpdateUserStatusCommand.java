package com.corwin.system.user.application.command;

import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;

/**
 * @author Corwin 2026/7/7
 */
public record BatchUpdateUserStatusCommand(
        List<Long> userIds,
        UserStatus status
) {
}
