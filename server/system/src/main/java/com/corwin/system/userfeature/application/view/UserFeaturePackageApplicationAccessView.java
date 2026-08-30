package com.corwin.system.userfeature.application.view;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import java.util.List;

/**
 * View object representing an application access entry within a package, including its feature
 * access scope.
 *
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackageApplicationAccessView(
    String applicationId,
    String applicationCode,
    String applicationName,
    ApplicationFeatureAccessScope featureAccessScope,
    List<String> featureIds,
    List<UserFeatureItemView> features) {}
