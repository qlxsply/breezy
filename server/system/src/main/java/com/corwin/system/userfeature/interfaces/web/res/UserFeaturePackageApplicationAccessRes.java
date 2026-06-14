package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;

import java.util.List;

/**
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
