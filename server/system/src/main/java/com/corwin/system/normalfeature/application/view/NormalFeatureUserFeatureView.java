package com.corwin.system.normalfeature.application.view;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureOverrideType;

/**
 * @author Corwin 2026/5/25
 */
public record NormalFeatureUserFeatureView(
        Long id,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes,
        boolean groupEnabled,
        boolean effectiveEnabled,
        NormalFeatureOverrideType overrideType
) {
}
