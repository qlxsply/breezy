package com.corwin.system.normalfeature.application.view;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public record NormalFeatureView(
        Long id,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
) {
}
