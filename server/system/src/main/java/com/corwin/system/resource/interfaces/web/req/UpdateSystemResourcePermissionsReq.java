package com.corwin.system.resource.interfaces.web.req;

import java.util.List;

/**
 * Request DTO for updating permission bindings on a system resource.
 *
 * @author Corwin 2026/6/29
 */
public record UpdateSystemResourcePermissionsReq(
        List<Long> permissionIds
) {
}
