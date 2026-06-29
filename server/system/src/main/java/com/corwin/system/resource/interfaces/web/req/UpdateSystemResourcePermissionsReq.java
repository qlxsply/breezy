package com.corwin.system.resource.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public record UpdateSystemResourcePermissionsReq(
        List<Long> permissionIds
) {
}
