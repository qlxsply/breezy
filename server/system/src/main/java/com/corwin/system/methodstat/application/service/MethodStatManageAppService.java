package com.corwin.system.methodstat.application.service;

import com.corwin.system.methodstat.application.view.MethodStatGlobalSwitchView;
import com.corwin.system.methodstat.application.view.MethodStatMethodSwitchView;
import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatManageAppService {

    private final MethodStatSwitchAppService switchAppService;
    private final MethodStatMetadataRepository metadataRepository;
    private final MethodStatAggregateRepository aggregateRepository;

    public MethodStatGlobalSwitchView getGlobalSwitch() {
        return new MethodStatGlobalSwitchView(switchAppService.isGlobalEnabled());
    }

    public MethodStatGlobalSwitchView setGlobalSwitch(boolean enabled) {
        switchAppService.setGlobalEnabled(enabled);
        return new MethodStatGlobalSwitchView(switchAppService.isGlobalEnabled());
    }

    public MethodStatMethodSwitchView getMethodSwitch(String keyValue) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        return new MethodStatMethodSwitchView(key.value(), switchAppService.isMethodEnabled(key));
    }

    public MethodStatMethodSwitchView setMethodSwitch(String keyValue, boolean enabled) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        switchAppService.setMethodEnabled(key, enabled);
        return new MethodStatMethodSwitchView(key.value(), switchAppService.isMethodEnabled(key));
    }

    public void setAllMethodSwitch(boolean enabled) {
        metadataRepository.findAll().forEach(item -> switchAppService.setMethodEnabled(item.key(), enabled));
    }

    public void clearMethodStats(String keyValue) {
        MethodStatKey key = parseKey(keyValue);
        requireMetadataExists(key);
        aggregateRepository.removeByKey(key);
    }

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
