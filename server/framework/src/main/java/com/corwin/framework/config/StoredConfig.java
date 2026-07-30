package com.corwin.framework.config;

import java.util.Objects;

/**
 * A persisted config item with its full metadata, including code, description,
 * value type, raw value, level, and scope.
 *
 * @param code        the config code
 * @param description the human-readable description
 * @param valueType   the expected value type
 * @param value       the raw string value
 * @param level       the config level (defaults to {@link ConfigLevel#SYSTEM})
 * @param scope       the config scope (defaults to {@link ConfigScope#FRAMEWORK})
 * @author Corwin 2026/5/5
 */
public record StoredConfig(
        String code,
        String description,
        ConfigValueType valueType,
        String value,
        ConfigLevel level,
        ConfigScope scope
) {

    public StoredConfig {
        Objects.requireNonNull(code, "code required");
        Objects.requireNonNull(description, "description required");
        Objects.requireNonNull(valueType, "valueType required");
        Objects.requireNonNull(value, "value required");
        level = Objects.requireNonNullElse(level, ConfigLevel.SYSTEM);
        scope = Objects.requireNonNullElse(scope, ConfigScope.FRAMEWORK);
    }

    public ConfigItem toItem() {
        ConfigItem item = new ConfigItem();
        item.key = code;
        item.value = value;
        item.type = valueType;
        return item;
    }

    public boolean personalized() {
        return level == ConfigLevel.USER;
    }
}
