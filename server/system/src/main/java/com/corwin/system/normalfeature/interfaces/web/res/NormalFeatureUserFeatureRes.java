package com.corwin.system.normalfeature.interfaces.web.res;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureOverrideType;

/**
 * @author Corwin 2026/5/25
 */
public record NormalFeatureUserFeatureRes(
        String id,
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
