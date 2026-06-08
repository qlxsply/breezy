package com.corwin.system.user.application.command;

import com.corwin.system.user.domain.model.UserStatus;

/**
 * @author Corwin 2026/1/22
 */
public record UpdateUserCommand(
        String nickname,
        UserStatus status
) {
}
