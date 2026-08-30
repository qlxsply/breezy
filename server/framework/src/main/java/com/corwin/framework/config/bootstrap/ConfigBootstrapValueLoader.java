package com.corwin.framework.config.bootstrap;

import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigBootstrapValueLoader {

  Map<String, RawConfigValue> load(ConfigurableEnvironment environment);
}
