package com.corwin.system.userfeature.interfaces.web.req;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import java.util.List;

/**
 * Request DTO for an application access entry within a package.
 *
 * @param applicationId the application ID
 * @param featureAccessScope the access scope (FULL or PARTIAL)
 * @param featureIds list of selected feature IDs for partial access
 * @author Corwin 2026/6/14
 */
public record ApplicationAccessReq(
    String applicationId,
    ApplicationFeatureAccessScope featureAccessScope,
    List<String> featureIds) {}
