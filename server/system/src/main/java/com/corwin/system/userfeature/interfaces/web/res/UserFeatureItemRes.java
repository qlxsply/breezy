package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for a feature item within an application.
 *
 * @param id the feature ID
 * @param applicationId the parent application ID
 * @param code the feature code
 * @param name the feature name
 * @param description the feature description
 * @param enabled whether the feature is enabled
 * @param permissionCodes list of bound permission codes
 * @author Corwin 2026/6/14
 */
public record UserFeatureItemRes(
    String id,
    String applicationId,
    String code,
    String name,
    String description,
    boolean enabled,
    List<String> permissionCodes) {}
