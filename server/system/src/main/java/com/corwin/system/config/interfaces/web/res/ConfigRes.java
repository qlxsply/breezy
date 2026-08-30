package com.corwin.system.config.interfaces.web.res;

import com.corwin.framework.config.definition.ConfigActivationPolicy;
import com.corwin.framework.config.definition.ConfigEditPolicy;
import com.corwin.framework.config.definition.ConfigFieldSpec;
import com.corwin.framework.config.runtime.ConfigValueSource;
import com.corwin.system.config.application.view.ConfigManagementStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/7/31
 */
public record ConfigRes(
    String key,
    String module,
    String group,
    String title,
    String description,
    int schemaVersion,
    long persistedRevision,
    long effectiveRevision,
    boolean configured,
    ConfigActivationPolicy activationPolicy,
    ConfigEditPolicy editPolicy,
    ConfigValueSource source,
    ConfigManagementStatus status,
    boolean pendingRestart,
    JsonNode effectiveValue,
    JsonNode persistedValue,
    JsonNode defaultValue,
    List<ConfigFieldSpec> fields,
    Map<String, Boolean> sensitiveValuePresence,
    String editorId,
    String loadWarning) {

  public ConfigRes {
    effectiveValue = effectiveValue.deepCopy();
    persistedValue = persistedValue.deepCopy();
    defaultValue = defaultValue.deepCopy();
    fields = List.copyOf(fields);
    sensitiveValuePresence = Map.copyOf(sensitiveValuePresence);
  }
}
