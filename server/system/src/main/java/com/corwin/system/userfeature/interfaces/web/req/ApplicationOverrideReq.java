package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 * Request DTO for an application-level access override.
 *
 * @param applicationId the application ID
 * @param overrideType the override type (ENABLE, DISABLE, or NONE)
 * @param featureAccessScope the feature access scope when overriding to ENABLE
 * @author Corwin 2026/6/14
 */
public record ApplicationOverrideReq(
    String applicationId,
    UserAccessOverrideType overrideType,
    ApplicationFeatureAccessScope featureAccessScope) {}
