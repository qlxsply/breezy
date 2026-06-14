package com.corwin.system.userfeature.application.command;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record SaveUserFeaturePackageCommand(
        String code,
        String name,
        UserApplicationPackageType packageType,
        String description,
        boolean enabled,
        boolean defaultPackage,
        List<ApplicationAccessCommand> applicationAccesses
) {
    public record ApplicationAccessCommand(
            String applicationId,
            ApplicationFeatureAccessScope featureAccessScope,
            List<String> featureIds
    ) {
    }
}
