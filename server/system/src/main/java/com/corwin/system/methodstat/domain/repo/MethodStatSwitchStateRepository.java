package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;

import java.util.Optional;

/**
 * @author Corwin 2026/3/25
 */
public interface MethodStatSwitchStateRepository {

    MethodStatGlobalSwitchState getGlobalSwitchState();

    void saveGlobalSwitchState(MethodStatGlobalSwitchState globalSwitchState);

    Optional<MethodStatMethodSwitchState> findMethodSwitchState(MethodStatKey key);

    void saveMethodSwitchState(MethodStatMethodSwitchState methodSwitchState);

    void clearAllMethodSwitchStates();
}
