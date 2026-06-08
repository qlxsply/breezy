package com.corwin.config;

import com.corwin.framework.config.ConfigDefinition;
import com.corwin.framework.config.ConfigDefinitionProvider;
import com.corwin.framework.config.ConfigScope;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Corwin 2026/5/5
 */
public class BusinessConfigDefinitionProvider implements ConfigDefinitionProvider {

    @Override
    public ConfigScope scope() {
        return ConfigScope.BUSINESS;
    }

    @Override
    public Collection<ConfigDefinition> getDefinitions() {
        return Arrays.asList(BusinessConfigKeys.values());
    }
}
