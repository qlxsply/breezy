package com.corwin.system.userfeature.application.command;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserAccessOverrideType;

/**
 * Command object for an application-level access override.
 *
 * @param applicationId      the application ID
 * @param overrideType       the override type
 * @param featureAccessScope the feature access scope when overriding to ENABLE
 * @author Corwin 2026/6/14
 */
public record ApplicationOverrideCommand(String applicationId, UserAccessOverrideType overrideType,
                                         ApplicationFeatureAccessScope featureAccessScope) {
}
