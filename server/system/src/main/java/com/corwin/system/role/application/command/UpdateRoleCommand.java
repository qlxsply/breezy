package com.corwin.system.role.application.command;

/**
 * @author Corwin 2026/1/23
 */
public record UpdateRoleCommand(
        String code,
        String name,
        Boolean enabled
) {
}
