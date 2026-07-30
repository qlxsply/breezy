package com.corwin.system.userfeature.interfaces.web.req;

import java.util.List;

/**
 * Request DTO for saving the full user feature management configuration.
 *
 * @param packageIds           list of package IDs to assign to the user
 * @param applicationOverrides list of application-level overrides
 * @param featureOverrides     list of feature-level overrides
 * @author Corwin 2026/6/14
 */
public record SaveUserFeatureUserManagementReq(
        List<String> packageIds,
        List<ApplicationOverrideReq> applicationOverrides,
        List<FeatureOverrideReq> featureOverrides
) {
}
