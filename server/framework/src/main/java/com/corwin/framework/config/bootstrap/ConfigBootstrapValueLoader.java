package com.corwin.framework.config.bootstrap;

import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigBootstrapValueLoader {

    Map<String, RawConfigValue> load(ConfigurableEnvironment environment);
}
