package com.corwin.framework.config.bootstrap;

import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigSpecCatalog;
import com.corwin.framework.config.error.ConfigLoadException;
import com.corwin.framework.config.runtime.ConfigRegistry;
import com.corwin.framework.config.runtime.ConfigSnapshot;
import com.corwin.framework.config.runtime.ConfigSnapshotFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Corwin 2026/7/30
 */
@Slf4j
public final class ConfigBootstrapEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  private static final String BOOTSTRAP_PROCESS_PROPERTY = "bootstrap.initialization.process";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.getProperty(BOOTSTRAP_PROCESS_PROPERTY, Boolean.class, false)) {
      log.info("Skip unified config loading during bootstrap initialization process");
      return;
    }

    Map<String, ConfigSpec<?>> specs = ConfigSpecCatalog.loadAll();
    ConfigBootstrapValueLoader loader = discoverLoader(resolveClassLoader(application));
    Map<String, RawConfigValue> storedValues = loader.load(environment);
    Map<String, ConfigSnapshot<?>> snapshots = createSnapshots(specs, storedValues);
    ConfigRegistry.initialize(snapshots);

    long overrideCount =
        snapshots.values().stream().filter(snapshot -> snapshot.revision() > 0).count();
    long orphanCount =
        storedValues.keySet().stream().filter(key -> !specs.containsKey(key)).count();
    log.info(
        "Unified config registry initialized: definitions={}, persisted={}, orphaned={}",
        snapshots.size(),
        overrideCount,
        orphanCount);
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 2;
  }

  private ConfigBootstrapValueLoader discoverLoader(ClassLoader classLoader) {
    var providers =
        ServiceLoader.load(ConfigBootstrapValueLoader.class, classLoader).stream().toList();
    if (providers.size() != 1) {
      throw new ConfigLoadException(
          "Exactly one ConfigBootstrapValueLoader is required, discovered: " + providers.size());
    }
    try {
      return providers.getFirst().get();
    } catch (RuntimeException ex) {
      throw new ConfigLoadException("Failed to create ConfigBootstrapValueLoader", ex);
    }
  }

  private ClassLoader resolveClassLoader(SpringApplication application) {
    ClassLoader classLoader = application.getClassLoader();
    return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
  }

  private Map<String, ConfigSnapshot<?>> createSnapshots(
      Map<String, ConfigSpec<?>> specs, Map<String, RawConfigValue> storedValues) {
    if (storedValues == null) {
      throw new ConfigLoadException("ConfigBootstrapValueLoader returned null");
    }
    var snapshots = new LinkedHashMap<String, ConfigSnapshot<?>>();
    specs.forEach((key, spec) -> snapshots.put(key, createSnapshot(spec, storedValues.get(key))));
    return Map.copyOf(snapshots);
  }

  private <T> ConfigSnapshot<T> createSnapshot(ConfigSpec<T> spec, RawConfigValue storedValue) {
    if (storedValue == null) {
      return ConfigSnapshotFactory.createDefault(spec);
    }
    if (!spec.key().value().equals(storedValue.configKey())) {
      throw new ConfigLoadException("Stored config key does not match definition: " + spec.key());
    }
    return ConfigSnapshotFactory.createStored(
        spec,
        storedValue.content(),
        storedValue.schemaVersion(),
        storedValue.revision(),
        storedValue.configured());
  }
}
