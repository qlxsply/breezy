package com.corwin.framework.config.definition;

import com.corwin.framework.config.codec.ConfigJsonCodec;
import com.corwin.framework.config.error.ConfigDefinitionException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * @author Corwin 2026/7/30
 */
public final class ConfigSpecCatalog {

    private static volatile Map<String, ConfigSpec<?>> cache;

    public static Map<String, ConfigSpec<?>> loadAll() {
        var current = cache;
        if (current != null) {
            return current;
        }
        synchronized (ConfigSpecCatalog.class) {
            if (cache == null) {
                cache = discover();
            }
            return cache;
        }
    }

    public static Optional<ConfigSpec<?>> find(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadAll().get(key));
    }

    static void clearForReload() {
        synchronized (ConfigSpecCatalog.class) {
            cache = null;
        }
    }

    private static Map<String, ConfigSpec<?>> discover() {
        var providers = ServiceLoader.load(ConfigSpecProvider.class).stream().toList();
        if (providers.isEmpty()) {
            throw new ConfigDefinitionException("No ConfigSpecProvider discovered");
        }

        var specs = new ArrayList<ConfigSpec<?>>();
        for (var provider : providers) {
            Collection<ConfigSpec<?>> provided = provider.get().getConfigSpecs();
            if (provided == null || provided.isEmpty()) {
                throw new ConfigDefinitionException(
                        "ConfigSpecProvider returned no definitions: " + provider.type().getName());
            }
            specs.addAll(provided);
        }

        specs.sort(Comparator.comparing(ConfigSpec<?>::module).thenComparing(ConfigSpec::group)
                .thenComparingInt(ConfigSpec::order).thenComparing(spec -> spec.key().value()));

        var result = new LinkedHashMap<String, ConfigSpec<?>>();
        for (var spec : specs) {
            validateDefinition(spec);
            var previous = result.putIfAbsent(spec.key().value(), spec);
            if (previous != null) {
                throw new ConfigDefinitionException("Duplicate config key: " + spec.key());
            }
        }
        return Map.copyOf(result);
    }

    private static void validateDefinition(ConfigSpec<?> spec) {
        Objects.requireNonNull(spec, "config spec required");
        Objects.requireNonNull(spec.key(), "config key required");
        requireText(spec.module(), "module", spec);
        requireText(spec.group(), "group", spec);
        requireText(spec.title(), "title", spec);
        Objects.requireNonNull(spec.valueClass(), "valueClass required");
        Objects.requireNonNull(spec.defaultValue(), "defaultValue required");
        Objects.requireNonNull(spec.activationPolicy(), "activationPolicy required");
        Objects.requireNonNull(spec.editPolicy(), "editPolicy required");
        Objects.requireNonNull(spec.invalidValuePolicy(), "invalidValuePolicy required");
        if (spec.schemaVersion() < 1) {
            throw invalid(spec, "schemaVersion must be greater than zero");
        }
        validateDefault(spec);
        validateFields(spec);
    }

    private static <T> void validateDefault(ConfigSpec<T> spec) {
        try {
            T defaultValue = spec.valueClass().cast(spec.defaultValue());
            String content = ConfigJsonCodec.serialize(defaultValue);
            T decoded = ConfigJsonCodec.deserialize(content, spec.valueClass());
            var violations = immutableViolations(spec.validateRuntime(decoded));
            if (!violations.isEmpty()) {
                throw invalid(spec, "defaultValue failed runtime validation: " + violations);
            }
        } catch (ConfigDefinitionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ConfigDefinitionException("Invalid defaultValue for " + spec.key(), ex);
        }
    }

    private static void validateFields(ConfigSpec<?> spec) {
        List<ConfigFieldSpec> fields = spec.fields() == null ? List.of() : List.copyOf(spec.fields());
        var paths = new LinkedHashMap<String, ConfigFieldSpec>();
        JsonNode defaultTree = ConfigJsonCodec.mapper().valueToTree(spec.defaultValue());
        for (var field : fields) {
            if (field == null) {
                throw invalid(spec, "field definition must not be null");
            }
            if (paths.putIfAbsent(field.path(), field) != null) {
                throw invalid(spec, "duplicate field path: " + field.path());
            }
            if (!pathExists(defaultTree, field.path())) {
                throw invalid(spec, "field path does not exist in value type: " + field.path());
            }
        }
    }

    private static boolean pathExists(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || !current.isObject() || !current.has(segment)) {
                return false;
            }
            current = current.get(segment);
        }
        return true;
    }

    private static List<ConfigViolation> immutableViolations(List<ConfigViolation> violations) {
        return violations == null ? List.of() : List.copyOf(violations);
    }

    private static void requireText(String value, String field, ConfigSpec<?> spec) {
        if (value == null || value.isBlank()) {
            throw invalid(spec, field + " must not be blank");
        }
    }

    private static ConfigDefinitionException invalid(ConfigSpec<?> spec, String message) {
        return new ConfigDefinitionException("Invalid config definition " + spec.key() + ": " + message);
    }

    private ConfigSpecCatalog() {
    }
}
