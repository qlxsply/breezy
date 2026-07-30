package com.corwin.system.resource.application.view;

import java.util.List;

/**
 * View object containing detailed permission info for the current user.
 *
 * @author Corwin 2026/3/30
 */
public record MyPermissionsDetailView(
        String username,
        List<String> roles,
        List<String> permissionCodes
) {
}
