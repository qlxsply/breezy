package com.corwin.system.userfeature.application.view;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;
import java.util.List;

/**
 * View object representing an application in the user feature management context, with
 * inherited/effective visibility and access scope details.
 *
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserApplicationView(
    String id,
    String code,
    String name,
    String description,
    String icon,
    String routePath,
    String componentPath,
    boolean enabled,
    boolean inheritedVisible,
    boolean effectiveVisible,
    String packageAccessScope,
    UserAccessOverrideType overrideType,
    ApplicationFeatureAccessScope overrideAccessScope,
    List<UserFeatureUserFeatureView> features) {}
