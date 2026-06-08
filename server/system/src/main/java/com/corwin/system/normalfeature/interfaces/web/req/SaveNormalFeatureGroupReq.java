package com.corwin.system.normalfeature.interfaces.web.req;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupType;

/**
 * @author Corwin 2026/5/21
 */
public record SaveNormalFeatureGroupReq(
        String code,
        String name,
        NormalFeatureGroupType groupType,
        String description,
        boolean enabled,
        boolean defaultGroup,
        List<String> featureIds
) {
}
