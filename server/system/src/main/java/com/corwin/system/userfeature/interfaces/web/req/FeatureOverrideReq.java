package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 *
 * @author Corwin 2026/6/14
 */
public record FeatureOverrideReq(
        String applicationId,
        String featureId,
        UserAccessOverrideType overrideType
) {
}
