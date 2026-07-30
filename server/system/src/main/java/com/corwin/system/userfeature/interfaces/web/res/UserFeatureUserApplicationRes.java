package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

import java.util.List;

/**
 * Response DTO for a user's application view with inherited/effective visibility and override details.
 *
 * @param id                 the application ID
 * @param code               the application code
 * @param name               the application name
 * @param description        the application description
 * @param icon               the application icon
 * @param routePath          the frontend route path
 * @param componentPath      the frontend component path
 * @param enabled            whether the application is enabled globally
 * @param inheritedVisible   whether access is inherited from package membership
 * @param effectiveVisible   whether the application is effectively visible
 * @param packageAccessScope aggregated access scope from packages
 * @param overrideType       the user-level override type
 * @param overrideAccessScope the override feature access scope
 * @param features           list of feature details under this application
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserApplicationRes(
        String id,
        String code,
        String name,
        String description,
        String icon,
        String routePath,
        String componentPath,
        boolean enabled,
        boolean inheritedVisible,
        boolean effectiveVisible,
        String packageAccessScope,
        UserAccessOverrideType overrideType,
        ApplicationFeatureAccessScope overrideAccessScope,
        List<UserFeatureUserFeatureRes> features
) {
}
