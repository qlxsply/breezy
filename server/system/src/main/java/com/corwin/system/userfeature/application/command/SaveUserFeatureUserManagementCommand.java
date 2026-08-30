package com.corwin.system.userfeature.application.command;

import java.util.List;

/**
 * Command object for saving the full user feature management configuration.
 *
 * @param packageIds list of package IDs to assign to the user
 * @param applicationOverrides list of application-level overrides
 * @param featureOverrides list of feature-level overrides
 * @author Corwin 2026/6/14
 */
public record SaveUserFeatureUserManagementCommand(
    List<String> packageIds,
    List<ApplicationOverrideCommand> applicationOverrides,
    List<FeatureOverrideCommand> featureOverrides) {}
