package com.corwin.system.config.application.config;

import com.corwin.framework.config.ConfigDefinition;
import com.corwin.framework.config.ConfigDefinitionProvider;
import com.corwin.framework.config.ConfigScope;

import java.util.Arrays;
import java.util.Collection;

/**
 * Provides system-level configuration definitions to the ConfigDefinitionCatalog.
 * Supplies all enum constants from {@link SystemConfigKeys} as config definitions
 * under the SYSTEM scope.
 *
 * @author Corwin 2026/5/5
 */
public class SystemConfigDefinitionProvider implements ConfigDefinitionProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigScope scope() {
        return ConfigScope.SYSTEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigDefinition> getDefinitions() {
        return Arrays.asList(SystemConfigKeys.values());
    }
}
