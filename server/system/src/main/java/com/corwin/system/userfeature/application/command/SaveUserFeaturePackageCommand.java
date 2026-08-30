package com.corwin.system.userfeature.application.command;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;
import java.util.List;

/**
 * Command object for creating or updating a user application package.
 *
 * @param code the package code
 * @param name the package name
 * @param packageType the package type
 * @param description the package description
 * @param enabled whether the package is enabled
 * @param defaultPackage whether this is a default package
 * @param applicationAccesses list of application access commands
 * @author Corwin 2026/6/14
 */
public record SaveUserFeaturePackageCommand(
    String code,
    String name,
    UserApplicationPackageType packageType,
    String description,
    boolean enabled,
    boolean defaultPackage,
    List<ApplicationAccessCommand> applicationAccesses) {
  /**
   * Command object for an application access entry within a package.
   *
   * @param applicationId the application ID
   * @param featureAccessScope the access scope (FULL or PARTIAL)
   * @param featureIds list of selected feature IDs for partial access
   */
  public record ApplicationAccessCommand(
      String applicationId,
      ApplicationFeatureAccessScope featureAccessScope,
      List<String> featureIds) {}
}
