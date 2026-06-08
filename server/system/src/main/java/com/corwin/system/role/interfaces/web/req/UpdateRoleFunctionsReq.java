package com.corwin.system.role.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public record UpdateRoleFunctionsReq(
        List<Long> functionIds
) {
}
