package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 * Request DTO for a feature-level access override.
 *
 * @param applicationId the application ID
 * @param featureId the feature ID
 * @param overrideType the override type (ENABLE, DISABLE, or NONE)
 * @author Corwin 2026/6/14
 */
public record FeatureOverrideReq(
    String applicationId, String featureId, UserAccessOverrideType overrideType) {}
