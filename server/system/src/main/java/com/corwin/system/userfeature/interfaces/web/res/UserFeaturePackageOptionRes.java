package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

/**
 * Response DTO for a package option in selection dropdowns.
 *
 * @param id             the package ID
 * @param code           the package code
 * @param name           the package name
 * @param packageType    the package type
 * @param description    the package description
 * @param enabled        whether the package is enabled
 * @param defaultPackage whether this is a default package
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
