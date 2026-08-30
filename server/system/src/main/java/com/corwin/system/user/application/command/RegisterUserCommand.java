package com.corwin.system.user.application.command;

/**
 * Command to register a new end-user account with username, nickname, and password.
 *
 * @author Corwin 2026/4/19
 */
public record RegisterUserCommand(String username, String nickname, String password) {}
