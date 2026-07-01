package com.corwin.system.user.application.command;

import java.util.List;

/**
 * @author Corwin 2026/1/22
 */
public record CreateUserCommand(
        String username,
        String nickname,
        String password,
        List<Long> roleIds
) {
}
