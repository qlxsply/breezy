package com.corwin.system.user.application.command;

/**
 * Command to update an end-user's profile (nickname).
 *
 * @author Corwin 2026/4/19
 */
public record UpdateMyProfileCommand(
        String nickname
) {
}
