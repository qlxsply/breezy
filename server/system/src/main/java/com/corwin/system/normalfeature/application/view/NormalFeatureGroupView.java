package com.corwin.system.normalfeature.application.view;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupType;

import java.util.List;

/**
 * @author Corwin 2026/5/21
 */
public record NormalFeatureGroupView(
        Long id,
        String code,
        String name,
        NormalFeatureGroupType groupType,
        String description,
        boolean enabled,
        boolean defaultGroup,
        List<String> featureIds,
        List<NormalFeatureItemView> features
) {
}
