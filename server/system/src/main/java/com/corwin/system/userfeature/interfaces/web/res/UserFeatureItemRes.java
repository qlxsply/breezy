package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureItemRes(
        String id,
        String applicationId,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
) {
}
