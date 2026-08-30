package com.corwin.system.methodstat.infrastructure.persistence;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;
import com.corwin.system.methodstat.domain.repo.MethodStatSwitchStateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link MethodStatSwitchStateRepository} using ConcurrentHashMap and
 * AtomicBoolean.
 *
 * @author Corwin 2026/3/25
 */
@Repository
public class InMemoryMethodStatSwitchStateRepository implements MethodStatSwitchStateRepository {

  private final AtomicBoolean globalEnabled = new AtomicBoolean(false);
  private final Map<String, MethodStatMethodSwitchState> methodSwitchStore =
      new ConcurrentHashMap<>();

  @Override
  public MethodStatGlobalSwitchState getGlobalSwitchState() {
    return new MethodStatGlobalSwitchState(globalEnabled.get());
  }

  /**
   * Persist the current global switch state.
   *
   * @param globalSwitchState the global switch state to persist
   */
  @Override
  public void saveGlobalSwitchState(MethodStatGlobalSwitchState globalSwitchState) {
    globalEnabled.set(globalSwitchState.enabled());
  }

  /**
   * Find the per-method switch state for the given key.
   *
   * @param key the method key
   * @return optional containing the switch state if present
   */
  @Override
  public Optional<MethodStatMethodSwitchState> findMethodSwitchState(MethodStatKey key) {
    return Optional.ofNullable(methodSwitchStore.get(key.value()));
  }

  /**
   * Persist the per-method switch state.
   *
   * @param methodSwitchState the method switch state to persist
   */
  @Override
  public void saveMethodSwitchState(MethodStatMethodSwitchState methodSwitchState) {
    methodSwitchStore.put(methodSwitchState.key().value(), methodSwitchState);
  }

  /** Remove all stored per-method switch states. */
  @Override
  public void clearAllMethodSwitchStates() {
    methodSwitchStore.clear();
  }
}
