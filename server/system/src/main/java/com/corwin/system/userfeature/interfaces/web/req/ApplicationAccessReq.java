package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;

import java.util.List;

/**
 *
 * @author Corwin 2026/6/14
 */
public record ApplicationAccessReq(
        String applicationId,
        ApplicationFeatureAccessScope featureAccessScope,
        List<String> featureIds
) {
}
