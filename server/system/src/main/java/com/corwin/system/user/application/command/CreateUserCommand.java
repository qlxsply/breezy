package com.corwin.system.user.application.command;

import java.util.List;

/**
 * Command to create a new admin user with username, nickname, password, and optional role assignments.
 *
 * @author Corwin 2026/1/22
 */
public record CreateUserCommand(
        String username,
        String nickname,
        String password,
        List<Long> roleIds
) {
}
