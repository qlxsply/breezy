package com.corwin.system.config.domain.model;

import com.corwin.framework.config.definition.ConfigKey;

import java.time.Instant;
import java.util.Objects;

/**
 * @author Corwin 2026/7/30
 */
public record ConfigValue(
        String configKey,
        String content,
        int schemaVersion,
        long revision,
        boolean configured,
        Long updatedBy,
        Instant updatedAt,
        String updateReason
) {

    private static final int UPDATE_REASON_MAX_LENGTH = 500;

    public ConfigValue {
        new ConfigKey(configKey);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be greater than zero");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
        Objects.requireNonNull(updatedAt, "updatedAt required");
        updateReason = normalizeReason(updateReason);
    }

    public static ConfigValue createOverride(String configKey, String content, int schemaVersion, Long updatedBy,
            Instant updatedAt, String updateReason) {
        return new ConfigValue(configKey, content, schemaVersion, 1, true, updatedBy, updatedAt, updateReason);
    }

    public ConfigValue updateOverride(String content, int schemaVersion, Long updatedBy, Instant updatedAt,
            String updateReason) {
        return new ConfigValue(configKey, content, schemaVersion, nextRevision(), true, updatedBy, updatedAt,
                updateReason);
    }

    public ConfigValue resetToDefault(String defaultContent, int defaultSchemaVersion, Long updatedBy,
            Instant updatedAt, String updateReason) {
        return new ConfigValue(configKey, defaultContent, defaultSchemaVersion, nextRevision(), false, updatedBy,
                updatedAt, updateReason);
    }

    private long nextRevision() {
        return Math.addExact(revision, 1);
    }

    private static String normalizeReason(String updateReason) {
        String value = updateReason == null ? "" : updateReason.trim();
        if (value.length() > UPDATE_REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("updateReason must not exceed 500 characters");
        }
        return value;
    }
}
