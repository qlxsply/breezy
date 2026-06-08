package com.corwin.system.user.application.command;

/**
 * @author Corwin 2026/4/19
 */
public record RegisterUserCommand(
        String username,
        String nickname,
        String password
) {
}
