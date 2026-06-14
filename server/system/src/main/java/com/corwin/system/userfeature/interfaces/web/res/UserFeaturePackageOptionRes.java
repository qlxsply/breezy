package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackageOptionRes(
        String id,
        String code,
        String name,
        UserApplicationPackageType packageType,
        String description,
        boolean enabled,
        boolean defaultPackage
) {
}
