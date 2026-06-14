package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserFeatureRes(
        String id,
        String applicationId,
        String applicationCode,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes,
        boolean inheritedEnabled,
        boolean effectiveEnabled,
        UserAccessOverrideType overrideType
) {
}
