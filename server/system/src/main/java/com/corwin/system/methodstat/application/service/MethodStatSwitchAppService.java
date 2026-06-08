package com.corwin.system.methodstat.application.service;

import com.corwin.system.methodstat.domain.model.MethodStatGlobalSwitchState;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatSwitchStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatSwitchAppService {

    private final MethodStatSwitchStateRepository switchStateRepository;
    private final MethodStatMetadataRepository metadataRepository;
    private final MethodStatAggregateRepository aggregateRepository;
    private final MethodStatMetadataCollector metadataCollector;

    public boolean isGlobalEnabled() {
        return switchStateRepository.getGlobalSwitchState().enabled();
    }

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

    public boolean isMethodEnabled(MethodStatKey key) {
        Objects.requireNonNull(key, "key required");
        return switchStateRepository.findMethodSwitchState(key).map(MethodStatMethodSwitchState::enabled).orElse(false);
    }

    public boolean ensureMethodSwitchState(MethodStatKey key, boolean defaultEnabled) {
        Objects.requireNonNull(key, "key required");
        return switchStateRepository.findMethodSwitchState(key).map(MethodStatMethodSwitchState::enabled)
                .orElseGet(() -> {
                    switchStateRepository.saveMethodSwitchState(new MethodStatMethodSwitchState(key, defaultEnabled));
                    return defaultEnabled;
                });
    }

    public void setMethodEnabled(MethodStatKey key, boolean enabled) {
        Objects.requireNonNull(key, "key required");
        switchStateRepository.saveMethodSwitchState(new MethodStatMethodSwitchState(key, enabled));
        metadataRepository.findByKey(key)
                .ifPresent(metadata -> metadataRepository.save(metadata.withMethodSwitchEnabled(enabled)));
    }

    public boolean isCollectEnabled(MethodStatKey key) {
        return isGlobalEnabled() && isMethodEnabled(key);
    }

}
