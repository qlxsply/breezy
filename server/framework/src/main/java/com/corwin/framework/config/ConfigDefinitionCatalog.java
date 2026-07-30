package com.corwin.framework.config;

import java.util.*;

/**
 * Catalog that discovers all {@link ConfigDefinitionProvider} implementations
 * via {@link java.util.ServiceLoader} and exposes a consolidated,
 * cached list of {@link ConfigDefinitionDescriptor} instances.
 *
 * @author Corwin 2026/5/5
 */
public final class ConfigDefinitionCatalog {

    private static volatile List<ConfigDefinitionDescriptor> cachedDefinitions;

    public static List<ConfigDefinitionDescriptor> loadAll() {
        List<ConfigDefinitionDescriptor> definitions = cachedDefinitions;
        if (definitions != null) {
            return definitions;
        }
        synchronized (ConfigDefinitionCatalog.class) {
            if (cachedDefinitions == null) {
                cachedDefinitions = List.copyOf(discoverAll());
            }
            return cachedDefinitions;
        }
    }

    public static Optional<ConfigDefinitionDescriptor> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return loadAll().stream().filter(item -> item.code().equalsIgnoreCase(normalized)).findFirst();
    }

    private static List<ConfigDefinitionDescriptor> discoverAll() {
        ServiceLoader<ConfigDefinitionProvider> loader = ServiceLoader.load(ConfigDefinitionProvider.class);
        Map<String, ConfigDefinitionDescriptor> byCode = new LinkedHashMap<>();
        for (ConfigDefinitionProvider provider : loader) {
            ConfigScope scope = Objects.requireNonNullElse(provider.scope(), ConfigScope.FRAMEWORK);
            for (ConfigDefinition definition : provider.getDefinitions()) {
                ConfigDefinitionDescriptor descriptor = ConfigDefinitionDescriptor.of(scope, definition);
                ConfigDefinitionDescriptor previous = byCode.putIfAbsent(descriptor.code(), descriptor);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate config definition code detected: " + descriptor.code());
                }
            }
        }

        List<ConfigDefinitionDescriptor> result = new ArrayList<>(byCode.values());
        result.sort(Comparator.comparing(ConfigDefinitionDescriptor::scope)
                .thenComparing(ConfigDefinitionDescriptor::code));
        return result;
    }

    private ConfigDefinitionCatalog() {
    }
}
