package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;

import java.util.List;

/**
 * Response DTO for an application access entry within a package.
 *
 * @param applicationId      the application ID
 * @param applicationCode    the application code
 * @param applicationName    the application name
 * @param featureAccessScope the access scope (FULL or PARTIAL)
 * @param featureIds         list of selected feature IDs for partial access
 * @param features           list of feature details
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackageApplicationAccessRes(
        String applicationId,
        String applicationCode,
        String applicationName,
        ApplicationFeatureAccessScope featureAccessScope,
        List<String> featureIds,
        List<UserFeatureItemRes> features
) {
}
