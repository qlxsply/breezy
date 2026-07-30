package com.corwin.framework.config;

import java.util.Objects;

/**
 * Immutable descriptor that combines a {@link ConfigDefinition} with its
 * originating {@link ConfigScope}, providing the fully-qualified metadata
 * for a single configuration item.
 *
 * @param code        the unique config code (derived from {@link ConfigDefinition#name()})
 * @param description the human-readable description
 * @param valueType   the expected value type
 * @param level       the config level (defaults to {@link ConfigLevel#SYSTEM})
 * @param scope       the config scope (defaults to {@link ConfigScope#FRAMEWORK})
 * @author Corwin 2026/5/5
 */
public record ConfigDefinitionDescriptor(
        String code,
        String description,
        ConfigValueType valueType,
        ConfigLevel level,
        ConfigScope scope
) {

    public ConfigDefinitionDescriptor {
        Objects.requireNonNull(code, "code required");
        Objects.requireNonNull(description, "description required");
        Objects.requireNonNull(valueType, "valueType required");
        level = Objects.requireNonNullElse(level, ConfigLevel.SYSTEM);
        scope = Objects.requireNonNullElse(scope, ConfigScope.FRAMEWORK);
    }

    public static ConfigDefinitionDescriptor of(ConfigScope scope, ConfigDefinition definition) {
        return new ConfigDefinitionDescriptor(definition.name(), definition.desc(), definition.valueType(),
                definition.level(), scope);
    }
}
