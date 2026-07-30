package com.corwin.system.role.application.command;

/**
 * Command for creating a new role.
 *
 * @author Corwin 2026/1/23
 */
public record CreateRoleCommand(
        String code,
        String name,
        Boolean enabled
) {
}
