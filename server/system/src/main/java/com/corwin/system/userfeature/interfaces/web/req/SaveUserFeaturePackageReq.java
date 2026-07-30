package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

import java.util.List;

/**
 * Request DTO for creating or updating a user application package.
 *
 * @param code               the package code
 * @param name               the package name
 * @param packageType        the package type
 * @param description        the package description
 * @param enabled            whether the package is enabled
 * @param defaultPackage     whether this is a default package
 * @param applicationAccesses list of application access entries
 * @author Corwin 2026/6/14
 */
public record SaveUserFeaturePackageReq(
        String code,
        String name,
        UserApplicationPackageType packageType,
        String description,
        boolean enabled,
        boolean defaultPackage,
        List<ApplicationAccessReq> applicationAccesses
) {
}
