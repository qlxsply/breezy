package com.corwin.framework.config;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Corwin 2026/5/5
 */
public class DefaultConfigDefinitionProvider implements ConfigDefinitionProvider {

    @Override
    public ConfigScope scope() {
        return ConfigScope.FRAMEWORK;
    }

    @Override
    public Collection<ConfigDefinition> getDefinitions() {
        return Arrays.asList(DefaultConfigKeys.values());
    }
}
