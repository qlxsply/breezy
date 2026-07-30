package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for the full user feature management view.
 *
 * @param userId       the user ID
 * @param account      the user account name
 * @param packageIds   IDs of packages assigned to the user
 * @param packages     available package options
 * @param applications application-level access info with per-feature details
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserManagementRes(
        String userId,
        String account,
        List<String> packageIds,
        List<UserFeaturePackageOptionRes> packages,
        List<UserFeatureUserApplicationRes> applications
) {
}
