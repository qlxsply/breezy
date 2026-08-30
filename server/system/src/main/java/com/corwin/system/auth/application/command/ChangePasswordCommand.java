package com.corwin.system.auth.application.command;

/**
 * Command object for changing a user's password.
 *
 * @author Corwin 2026/1/23
 */
public record ChangePasswordCommand(String oldPassword, String newPassword) {}
