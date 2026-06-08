package com.corwin.system.normalfeature.application.command;

import java.util.List;

import com.corwin.system.normalfeature.domain.model.NormalFeatureOverrideType;

/**
 * @author Corwin 2026/5/25
 */
public record SaveNormalFeatureUserManagementCommand(
        List<String> groupIds,
        List<FeatureOverrideCommand> overrides
) {
    public record FeatureOverrideCommand(
            String featureId,
            NormalFeatureOverrideType overrideType
    ) {
    }
}
