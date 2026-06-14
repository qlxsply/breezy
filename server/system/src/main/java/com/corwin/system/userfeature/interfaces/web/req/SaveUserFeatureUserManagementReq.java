package com.corwin.system.userfeature.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record SaveUserFeatureUserManagementReq(
        List<String> packageIds,
        List<ApplicationOverrideReq> applicationOverrides,
        List<FeatureOverrideReq> featureOverrides
) {
}
