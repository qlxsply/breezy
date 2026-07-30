package com.corwin.system.userfeature.application.view;

import java.util.List;

/**
 * View object representing a feature item with its bound permission codes.
 *
 * @author Corwin 2026/6/14
 */
public record UserFeatureItemView(
        Long id,
        Long applicationId,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
) {
}
