package com.corwin.system.normalfeature.interfaces.web.req;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureOverrideType;

/**
 * @author Corwin 2026/5/25
 */
public record SaveNormalFeatureUserManagementReq(
        List<String> groupIds,
        List<FeatureOverrideReq> overrides
) {
    public record FeatureOverrideReq(
            String featureId,
            NormalFeatureOverrideType overrideType
    ) {
    }
}
