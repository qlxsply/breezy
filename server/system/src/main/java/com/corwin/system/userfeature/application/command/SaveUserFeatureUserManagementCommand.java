package com.corwin.system.userfeature.application.command;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record SaveUserFeatureUserManagementCommand(List<String> packageIds,
                                                   List<ApplicationOverrideCommand> applicationOverrides,
                                                   List<FeatureOverrideCommand> featureOverrides) {


}
