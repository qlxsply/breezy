package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;

import java.util.Optional;

/**
 * Repository interface for managing global and per-method statistics switch states.
 * @author Corwin 2026/3/25
 */
public interface MethodStatSwitchStateRepository {

    /**
     * Retrieve the current global switch state.
     * @return the global switch state
     */
    MethodStatGlobalSwitchState getGlobalSwitchState();

    /**
     * Persist the global switch state.
     * @param globalSwitchState the global switch state to persist
     */
    void saveGlobalSwitchState(MethodStatGlobalSwitchState globalSwitchState);

    /**
     * Find the per-method switch state for the given key.
     * @param key the method key
     * @return optional containing the switch state if present
     */
    Optional<MethodStatMethodSwitchState> findMethodSwitchState(MethodStatKey key);

    /**
     * Persist the per-method switch state.
     * @param methodSwitchState the method switch state to persist
     */
    void saveMethodSwitchState(MethodStatMethodSwitchState methodSwitchState);

    /**
     * Remove all stored per-method switch states.
     */
    void clearAllMethodSwitchStates();
}
