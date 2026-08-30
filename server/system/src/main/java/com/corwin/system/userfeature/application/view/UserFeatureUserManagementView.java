package com.corwin.system.userfeature.application.view;

import java.util.List;

/**
 * View object representing the full user feature management data for the admin console.
 *
 * @param userId the user ID
 * @param account the user account name
 * @param packageIds IDs of packages assigned to the user
 * @param packages available package options
 * @param applications application-level access info with per-feature details
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserManagementView(
    String userId,
    String account,
    List<String> packageIds,
    List<UserFeaturePackageOptionView> packages,
    List<UserFeatureUserApplicationView> applications) {}
