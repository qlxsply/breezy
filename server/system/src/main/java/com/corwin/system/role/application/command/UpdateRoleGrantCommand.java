package com.corwin.system.role.application.command;

import java.util.List;

/**
 * Command for updating the resource grants assigned to a role.
 *
 * @author Corwin 2026/5/19
 */
public record UpdateRoleGrantCommand(List<Long> resourceIds) {}
