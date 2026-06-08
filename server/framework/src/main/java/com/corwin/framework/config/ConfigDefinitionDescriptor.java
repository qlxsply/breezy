package com.corwin.framework.config;

import java.util.Objects;

/**
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
