package com.corwin.system.userfeature.interfaces.web.res;

import java.util.List;

/**
 * Response DTO for a product application with its features.
 *
 * @param id the application ID
 * @param code the application code
 * @param name the application name
 * @param description the application description
 * @param icon the application icon
 * @param routePath the frontend route path
 * @param componentPath the frontend component path
 * @param enabled whether the application is enabled
 * @param featureCount number of features under this application
 * @param permissionBindingCount number of permission bindings
 * @param features list of features in this application
 * @author Corwin 2026/6/14
 */
public record UserFeatureApplicationRes(
    String id,
    String code,
    String name,
    String description,
    String icon,
    String routePath,
    String componentPath,
    boolean enabled,
    int featureCount,
    int permissionBindingCount,
    List<UserFeatureItemRes> features) {}
