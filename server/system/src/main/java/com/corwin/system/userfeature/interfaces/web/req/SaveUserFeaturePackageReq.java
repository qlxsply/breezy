package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

import java.util.List;

/**
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
