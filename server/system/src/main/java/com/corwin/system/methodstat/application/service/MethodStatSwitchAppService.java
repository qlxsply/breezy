package com.corwin.system.methodstat.application.service;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatSwitchStateRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for managing global and per-method statistics collection switches.
 *
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatSwitchAppService {

  private final MethodStatSwitchStateRepository switchStateRepository;
  private final MethodStatMetadataRepository metadataRepository;
  private final MethodStatAggregateRepository aggregateRepository;
  private final MethodStatMetadataCollector metadataCollector;

  /**
   * Check whether the global statistics collection switch is currently enabled.
   *
   * @return true if global collection is enabled
   */
  public boolean isGlobalEnabled() {
    return switchStateRepository.getGlobalSwitchState().enabled();
  }

  /**
   * Enable or disable the global statistics collection. When enabling, all pointcut metadata is
   * collected first; when disabling, all stored data is cleared.
   *
   * @param enabled true to enable global collection, false to disable
   */
  public void setGlobalEnabled(boolean enabled) {
    if (enabled) {
      if (isGlobalEnabled()) {
        return;
      }
      metadataCollector.collectAllPointcutMetadata();
      switchStateRepository.saveGlobalSwitchState(new MethodStatGlobalSwitchState(true));
      return;
    }
    switchStateRepository.saveGlobalSwitchState(new MethodStatGlobalSwitchState(enabled));
    aggregateRepository.clearAll();
    metadataRepository.clearAll();
    switchStateRepository.clearAllMethodSwitchStates();
  }

  /**
   * Check whether the per-method statistics switch is enabled for the given key.
   *
   * @param key the method identifier
   * @return true if the method switch is enabled, defaults to false
   */
  public boolean isMethodEnabled(MethodStatKey key) {
    Objects.requireNonNull(key, "key required");
    return switchStateRepository
        .findMethodSwitchState(key)
        .map(MethodStatMethodSwitchState::enabled)
        .orElse(false);
  }

  /**
   * Ensure a method switch state exists, creating it with the given default if absent.
   *
   * @param key the method identifier
   * @param defaultEnabled default enabled state used when no existing state is found
   * @return the effective enabled state
   */
  public boolean ensureMethodSwitchState(MethodStatKey key, boolean defaultEnabled) {
    Objects.requireNonNull(key, "key required");
    return switchStateRepository
        .findMethodSwitchState(key)
        .map(MethodStatMethodSwitchState::enabled)
        .orElseGet(
            () -> {
              switchStateRepository.saveMethodSwitchState(
                  new MethodStatMethodSwitchState(key, defaultEnabled));
              return defaultEnabled;
            });
  }

  /**
   * Explicitly set the per-method switch state and update metadata accordingly.
   *
   * @param key the method identifier
   * @param enabled the new enabled state
   */
  public void setMethodEnabled(MethodStatKey key, boolean enabled) {
    Objects.requireNonNull(key, "key required");
    switchStateRepository.saveMethodSwitchState(new MethodStatMethodSwitchState(key, enabled));
    metadataRepository
        .findByKey(key)
        .ifPresent(metadata -> metadataRepository.save(metadata.withMethodSwitchEnabled(enabled)));
  }

  /**
   * Determine whether collection is effectively enabled for the given key, requiring both the
   * global switch and the per-method switch to be enabled.
   *
   * @param key the method identifier
   * @return true if both global and method switches are enabled
   */
  public boolean isCollectEnabled(MethodStatKey key) {
    return isGlobalEnabled() && isMethodEnabled(key);
  }
}
