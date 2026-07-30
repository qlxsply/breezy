package com.corwin.system.userfeature.application.view;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

import java.util.List;

/**
 * View object representing a user application package with its application access details.
 *
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
