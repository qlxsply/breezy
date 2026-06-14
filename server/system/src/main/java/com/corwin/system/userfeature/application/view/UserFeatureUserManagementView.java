package com.corwin.system.userfeature.application.view;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserManagementView(
        String userId,
        String account,
        List<String> packageIds,
        List<UserFeaturePackageOptionView> packages,
        List<UserFeatureUserApplicationView> applications
) {
}
