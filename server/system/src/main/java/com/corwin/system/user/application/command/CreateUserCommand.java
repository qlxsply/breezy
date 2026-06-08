package com.corwin.system.user.application.command;

/**
 * @author Corwin 2026/1/22
 */
public record CreateUserCommand(
        String username,
        String nickname,
        String password
) {
}
