package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

import java.util.List;

/**
 * Response DTO for a user's feature view with inherited/effective enabled state and override type.
 *
 * @param id                the feature ID
 * @param applicationId     the parent application ID
 * @param applicationCode   the parent application code
 * @param code              the feature code
 * @param name              the feature name
 * @param description       the feature description
 * @param enabled           whether the feature is enabled globally
 * @param permissionCodes   list of bound permission codes
 * @param inheritedEnabled  whether access is inherited from packages
 * @param effectiveEnabled  whether the feature is effectively enabled
 * @param overrideType      the user-level override type
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
