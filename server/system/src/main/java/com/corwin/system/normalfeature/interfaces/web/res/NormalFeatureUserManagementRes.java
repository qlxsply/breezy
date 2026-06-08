package com.corwin.system.normalfeature.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/5/25
 */
public record NormalFeatureUserManagementRes(
        String userId,
        String account,
        List<String> groupIds,
        List<NormalFeatureUserFeatureRes> features
) {
}
