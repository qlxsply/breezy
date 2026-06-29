package com.corwin.system.role.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/5/19
 */
public record RoleGrantSelectionRes(
        List<String> resourceIds
) {
}
