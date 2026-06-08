package com.corwin.system.role.application.command;

import java.util.List;

/**
 * @author Corwin 2026/5/19
 */
public record UpdateRoleGrantCommand(
        List<Long> menuIds,
        List<Long> functionIds
) {
}
