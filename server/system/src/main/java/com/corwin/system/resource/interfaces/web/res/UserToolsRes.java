package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/6/6
 */
public record UserToolsRes(
        List<UserToolPageRes> tools,
        List<String> permissionCodes
) {
}
