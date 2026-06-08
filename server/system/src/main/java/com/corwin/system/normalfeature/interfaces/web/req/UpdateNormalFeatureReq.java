package com.corwin.system.normalfeature.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/4/20
 */
public record UpdateNormalFeatureReq(
        List<String> featureIds,
        List<String> resourceIds
) {

    public List<String> resolvedFeatureIds() {
        if (featureIds != null) {
            return featureIds;
        }
        if (resourceIds != null) {
            return resourceIds;
        }
        return List.of();
    }
}
