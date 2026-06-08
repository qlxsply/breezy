package com.corwin.system.normalfeature.application.command;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupType;

/**
 * @author Corwin 2026/5/21
 */
public record SaveNormalFeatureGroupCommand(
        String code,
        String name,
        NormalFeatureGroupType groupType,
        String description,
        boolean enabled,
        boolean defaultGroup,
        List<String> featureIds
) {
}
