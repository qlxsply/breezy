package com.corwin.system.userfeature.application.command;

import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 * Command object for a feature-level access override.
 *
 * @param applicationId the application ID
 * @param featureId the feature ID
 * @param overrideType the override type
 * @author Corwin 2026/6/14
 */
public record FeatureOverrideCommand(
    String applicationId, String featureId, UserAccessOverrideType overrideType) {}
