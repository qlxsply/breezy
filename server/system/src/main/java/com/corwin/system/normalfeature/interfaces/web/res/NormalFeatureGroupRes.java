package com.corwin.system.normalfeature.interfaces.web.res;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupType;

/**
 * @author Corwin 2026/5/21
 */
public record NormalFeatureGroupRes(
        String id,
        String code,
        String name,
        NormalFeatureGroupType groupType,
        String description,
        boolean enabled,
        boolean defaultGroup,
        List<String> featureIds,
        List<NormalFeatureItemRes> features
) {
}
