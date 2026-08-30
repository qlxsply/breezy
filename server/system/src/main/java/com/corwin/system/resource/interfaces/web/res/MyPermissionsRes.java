package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO containing the current user's granted permission codes.
 *
 * @author Corwin 2026/2/2
 */
public record MyPermissionsRes(List<String> permissionCodes) {}
