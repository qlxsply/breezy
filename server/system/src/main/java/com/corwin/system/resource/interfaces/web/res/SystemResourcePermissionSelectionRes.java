package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * Response DTO containing the permission IDs selected for a resource.
 *
 * @author Corwin 2026/6/29
 */
public record SystemResourcePermissionSelectionRes(
        List<String> permissionIds
) {
}
