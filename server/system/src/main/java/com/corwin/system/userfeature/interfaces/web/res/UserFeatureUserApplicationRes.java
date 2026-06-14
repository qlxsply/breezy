package com.corwin.system.userfeature.interfaces.web.res;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public record UserFeatureUserApplicationRes(
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
        List<UserFeatureUserFeatureRes> features
) {
}
