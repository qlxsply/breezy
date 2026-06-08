package com.corwin.system.user.application.command;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
public record UpdateUserRolesCommand(
        List<Long> roleIds
) {
}
