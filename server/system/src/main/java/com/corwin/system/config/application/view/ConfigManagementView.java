package com.corwin.system.config.application.view;

import com.corwin.framework.config.definition.ConfigActivationPolicy;
import com.corwin.framework.config.definition.ConfigEditPolicy;
import com.corwin.framework.config.definition.ConfigFieldSpec;
import com.corwin.framework.config.runtime.ConfigValueSource;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigManagementView(
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
        String loadWarning
) {

    public ConfigManagementView {
        fields = List.copyOf(fields);
        sensitiveValuePresence = Map.copyOf(sensitiveValuePresence);
        effectiveValue = effectiveValue.deepCopy();
        persistedValue = persistedValue.deepCopy();
        defaultValue = defaultValue.deepCopy();
    }
}
