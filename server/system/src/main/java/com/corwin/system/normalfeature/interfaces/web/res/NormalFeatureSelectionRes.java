package com.corwin.system.normalfeature.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/4/20
 */
public record NormalFeatureSelectionRes(
        List<String> featureIds,
        List<String> resourceIds
) {

    public NormalFeatureSelectionRes(List<String> featureIds) {
        this(featureIds, featureIds);
    }
}
