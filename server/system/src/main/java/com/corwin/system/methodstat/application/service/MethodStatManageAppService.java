package com.corwin.system.methodstat.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.methodstat.application.view.MethodStatGlobalSwitchView;
import com.corwin.system.methodstat.application.view.MethodStatMethodSwitchView;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for managing global and per-method switches and clearing statistics data.
 *
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatManageAppService {

    private final MethodStatSwitchAppService switchAppService;
    private final MethodStatMetadataRepository metadataRepository;
    private final MethodStatAggregateRepository aggregateRepository;

    /**
     * Retrieve the current global switch state.
     *
     * @return the global switch view
     */
    public MethodStatGlobalSwitchView getGlobalSwitch() {
        return new MethodStatGlobalSwitchView(switchAppService.isGlobalEnabled());
    }

    /**
     * Set the global switch state and return the updated view.
     *
     * @param enabled true to enable, false to disable
     * @return updated global switch view
     */
    public MethodStatGlobalSwitchView setGlobalSwitch(boolean enabled) {
        switchAppService.setGlobalEnabled(enabled);
        return new MethodStatGlobalSwitchView(switchAppService.isGlobalEnabled());
    }

    /**
     * Retrieve the per-method switch state for the given key.
     *
     * @param keyValue the method key string
     * @return the method switch view
     */
    public MethodStatMethodSwitchView getMethodSwitch(String keyValue) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        return new MethodStatMethodSwitchView(key.value(), switchAppService.isMethodEnabled(key));
    }

    /**
     * Set the per-method switch state for the given key.
     *
     * @param keyValue the method key string
     * @param enabled  the new enabled state
     * @return updated method switch view
     */
    public MethodStatMethodSwitchView setMethodSwitch(String keyValue, boolean enabled) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        switchAppService.setMethodEnabled(key, enabled);
        return new MethodStatMethodSwitchView(key.value(), switchAppService.isMethodEnabled(key));
    }

    /**
     * Set the switch state for all registered methods at once.
     *
     * @param enabled the new enabled state for all methods
     */
    public void setAllMethodSwitch(boolean enabled) {
        metadataRepository.findAll().forEach(item -> switchAppService.setMethodEnabled(item.key(), enabled));
    }

    /**
     * Clear aggregated statistics for a specific method by its key.
     *
     * @param keyValue the method key string
     */
    public void clearMethodStats(String keyValue) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        aggregateRepository.removeByKey(key);
    }

    /**
     * Clear all aggregated statistics across every method.
     */
    public void clearAllStats() {
        aggregateRepository.clearAll();
    }

    private MethodStatKey parseKey(String keyValue) {
        BizAssert.notBlank(keyValue, BaseError.MISSING_PARAMETER);
        return MethodStatKey.of(keyValue.trim());
    }

    private void requireMetadataExists(MethodStatKey key) {
        if (metadataRepository.findByKey(key).isEmpty()) {
            throw new BizException("Method metadata not found: " + key.value(), BaseError.NOT_FOUND);
        }
    }
}
