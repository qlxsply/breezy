package com.corwin.system.methodstat.infrastructure.persistence;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;
import com.corwin.system.methodstat.domain.repo.MethodStatSwitchStateRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Corwin 2026/3/25
 */
@Repository
public class InMemoryMethodStatSwitchStateRepository implements MethodStatSwitchStateRepository {

    private final AtomicBoolean globalEnabled = new AtomicBoolean(false);
    private final Map<String, MethodStatMethodSwitchState> methodSwitchStore = new ConcurrentHashMap<>();

    @Override
    public MethodStatGlobalSwitchState getGlobalSwitchState() {
        return new MethodStatGlobalSwitchState(globalEnabled.get());
    }

    @Override
    public void saveGlobalSwitchState(MethodStatGlobalSwitchState globalSwitchState) {
        globalEnabled.set(globalSwitchState.enabled());
    }

    @Override
    public Optional<MethodStatMethodSwitchState> findMethodSwitchState(MethodStatKey key) {
        return Optional.ofNullable(methodSwitchStore.get(key.value()));
    }

    @Override
    public void saveMethodSwitchState(MethodStatMethodSwitchState methodSwitchState) {
        methodSwitchStore.put(methodSwitchState.key().value(), methodSwitchState);
    }

    @Override
    public void clearAllMethodSwitchStates() {
        methodSwitchStore.clear();
    }
}
