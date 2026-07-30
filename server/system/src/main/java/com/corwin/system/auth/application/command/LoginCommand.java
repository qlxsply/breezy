package com.corwin.system.auth.application.command;

/**
 * Command object containing login credentials.
 *
 * @author Corwin 2026/1/22
 */
public record LoginCommand(
        String account,
        String password
) {
}
