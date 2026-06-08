package com.corwin.system.auth.application.command;

/**
 * @author Corwin 2026/1/23
 */
public record ChangePasswordCommand(
        String oldPassword,
        String newPassword
) {
}
