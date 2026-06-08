package com.corwin.system.normalfeature.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public record NormalFeatureRes(
        String id,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
) {
}
