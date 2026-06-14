package com.corwin.system.userfeature.application.view;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackageView(
        Long id,
        String code,
        String name,
        UserApplicationPackageType packageType,
        String description,
        boolean enabled,
        boolean defaultPackage,
        List<UserFeaturePackageApplicationAccessView> applicationAccesses
) {
}
