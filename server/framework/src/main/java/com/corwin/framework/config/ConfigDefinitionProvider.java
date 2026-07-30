package com.corwin.framework.config;

import java.util.Collection;

/**
 * SPI interface for contributing config definitions to the system catalog.
 *
 * @author Corwin 2026/5/5
 */
public interface ConfigDefinitionProvider {

    ConfigScope scope();

    Collection<ConfigDefinition> getDefinitions();
}
