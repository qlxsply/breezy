package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/2/2
 */
public record MyPermissionsDetailRes(
        String username,
        List<String> roles,
        List<String> permissionCodes
) {
}
