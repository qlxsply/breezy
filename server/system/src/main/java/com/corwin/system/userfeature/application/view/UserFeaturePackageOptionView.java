package com.corwin.system.userfeature.application.view;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;

/**
 * View object representing a package option for selection dropdowns in the user management UI.
 *
 * @author Corwin 2026/6/14
 */
public record UserFeaturePackageOptionView(
    String id,
    String code,
    String name,
    UserApplicationPackageType packageType,
    String description,
    boolean enabled,
    boolean defaultPackage) {}
