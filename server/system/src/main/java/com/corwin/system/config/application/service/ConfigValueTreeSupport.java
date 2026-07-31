package com.corwin.system.config.application.service;

import com.corwin.framework.config.definition.ConfigFieldSpec;
import com.corwin.framework.config.definition.ConfigViolation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/7/30
 */
final class ConfigValueTreeSupport {

    static JsonNode redact(JsonNode value, List<ConfigFieldSpec> fields) {
        JsonNode redacted = value.deepCopy();
        fields.stream().filter(ConfigFieldSpec::sensitive)
                .forEach(field -> setPath(redacted, field.path(), NullNode.getInstance(), false));
        return redacted;
    }

    static Map<String, Boolean> sensitivePresence(JsonNode value, List<ConfigFieldSpec> fields) {
        var presence = new LinkedHashMap<String, Boolean>();
        fields.stream().filter(ConfigFieldSpec::sensitive)
                .forEach(field -> presence.put(field.path(), hasValue(readPath(value, field.path()))));
        return Map.copyOf(presence);
    }

    static MergeResult mergeSensitive(JsonNode submitted, JsonNode current, boolean currentReadable,
            List<ConfigFieldSpec> fields) {
        JsonNode merged = submitted.deepCopy();
        var violations = new ArrayList<ConfigViolation>();
        for (ConfigFieldSpec field : fields) {
            if (!field.sensitive()) {
                continue;
            }
            JsonNode submittedValue = readPath(merged, field.path());
            if (submittedValue == null) {
                if (!currentReadable) {
                    violations.add(violation(field.path(), "SENSITIVE_VALUE_REQUIRED",
                            "Existing sensitive value cannot be preserved; submit a replacement value"));
                    continue;
                }
                JsonNode currentValue = readPath(current, field.path());
                if (currentValue != null) {
                    setPath(merged, field.path(), currentValue.deepCopy(), true);
                }
                continue;
            }
            if (submittedValue.isTextual() && submittedValue.textValue().matches("\\*+")) {
                violations.add(violation(field.path(), "MASKED_VALUE_NOT_ALLOWED",
                        "Masked sensitive value cannot be submitted"));
            }
        }
        violations.addAll(requiredViolations(merged, fields));
        return new MergeResult(merged, violations);
    }

    private static List<ConfigViolation> requiredViolations(JsonNode value, List<ConfigFieldSpec> fields) {
        var violations = new ArrayList<ConfigViolation>();
        for (ConfigFieldSpec field : fields) {
            JsonNode fieldValue = readPath(value, field.path());
            if (field.required() && (fieldValue == null || fieldValue.isNull())) {
                violations.add(violation(field.path(), "REQUIRED", field.title() + " is required"));
            }
        }
        return violations;
    }

    private static ConfigViolation violation(String path, String code, String message) {
        return new ConfigViolation(path, code, message);
    }

    private static boolean hasValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        return !value.isTextual() || !value.textValue().isBlank();
    }

    private static JsonNode readPath(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || !current.isObject() || !current.has(segment)) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }

    private static void setPath(JsonNode root, String path, JsonNode value, boolean createParents) {
        if (!(root instanceof ObjectNode objectRoot)) {
            return;
        }
        String[] segments = path.split("\\.");
        ObjectNode parent = objectRoot;
        for (int index = 0; index < segments.length - 1; index++) {
            JsonNode child = parent.get(segments[index]);
            if (child instanceof ObjectNode objectChild) {
                parent = objectChild;
                continue;
            }
            if (!createParents) {
                return;
            }
            parent = parent.putObject(segments[index]);
        }
        if (createParents || parent.has(segments[segments.length - 1])) {
            parent.set(segments[segments.length - 1], value);
        }
    }

    record MergeResult(
            JsonNode value,
            List<ConfigViolation> violations
    ) {

        MergeResult {
            value = value.deepCopy();
            violations = List.copyOf(violations);
        }
    }

    private ConfigValueTreeSupport() {
    }
}
