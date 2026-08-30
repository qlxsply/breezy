package com.corwin.framework.config.definition;

import java.util.Collection;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigSpecProvider {

  Collection<ConfigSpec<?>> getConfigSpecs();
}
