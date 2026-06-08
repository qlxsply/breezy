package com.corwin.system.role.application.command;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public record UpdateRoleFunctionsCommand(
        List<Long> functionIds
) {
}
