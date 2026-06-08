package com.corwin.system.normalfeature.application.view;

import java.util.List;

/**
 * @author Corwin 2026/5/25
 */
public record NormalFeatureUserManagementView(
        Long userId,
        String account,
        List<String> groupIds,
        List<NormalFeatureUserFeatureView> features
) {
}
