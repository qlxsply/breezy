package com.corwin.system.userfeature.application.command;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 *
 * @author Corwin 2026/6/14
 */
public record FeatureOverrideCommand(String applicationId, String featureId, UserAccessOverrideType overrideType) {
}
