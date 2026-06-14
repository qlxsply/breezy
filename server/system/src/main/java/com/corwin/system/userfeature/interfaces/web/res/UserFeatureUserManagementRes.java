package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
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
