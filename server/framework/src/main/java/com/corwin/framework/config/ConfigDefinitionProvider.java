package com.corwin.framework.config;

import java.util.Collection;

/**
 * @author Corwin 2026/5/5
 */
public interface ConfigDefinitionProvider {

    ConfigScope scope();

    Collection<ConfigDefinition> getDefinitions();
}
