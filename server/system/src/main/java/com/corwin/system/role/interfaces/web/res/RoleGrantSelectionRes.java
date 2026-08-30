package com.corwin.system.role.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for the currently selected resource IDs of a role grant.
 *
 * @author Corwin 2026/5/19
 */
public record RoleGrantSelectionRes(List<String> resourceIds) {}
