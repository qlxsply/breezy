package com.corwin.system.config.application.service;

import com.corwin.framework.config.codec.ConfigJsonCodec;
import com.corwin.framework.config.definition.*;
import com.corwin.framework.config.error.ConfigValidationException;
import com.corwin.framework.config.error.ConfigVersionConflictException;
import com.corwin.framework.config.runtime.ConfigRegistry;
import com.corwin.framework.config.runtime.ConfigSnapshot;
import com.corwin.framework.config.runtime.ConfigSnapshotFactory;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.config.application.command.ResetConfigCommand;
import com.corwin.system.config.application.command.UpdateConfigCommand;
import com.corwin.system.config.application.command.ValidateConfigCommand;
import com.corwin.system.config.application.view.ConfigChangeView;
import com.corwin.system.config.application.view.ConfigValidationView;
import com.corwin.system.config.domain.model.ConfigValue;
import com.corwin.system.config.domain.repo.ConfigValueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Corwin 2026/7/30
 */
@Service
@RequiredArgsConstructor
public class ConfigCommandService {

    private final ConfigValueRepository configValueRepository;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public ConfigValidationView validate(ValidateConfigCommand command) {
        ConfigSpec<?> spec = requireEditableSpec(command.configKey());
        ConfigValue current = configValueRepository.findByConfigKey(spec.key().value()).orElse(null);
        PreparedConfig<?> prepared = prepare(spec, command.value(), current);
        return new ConfigValidationView(prepared.violations().isEmpty(), prepared.violations());
    }

    @Transactional
    public ConfigChangeView update(UpdateConfigCommand command) {
        if (command.expectedRevision() < 0) {
            throw new BizException("Invalid config update command", BaseError.INVALID_PARAMETER);
        }
        ConfigSpec<?> spec = requireEditableSpec(command.configKey());
        ConfigValue current = configValueRepository.findByConfigKey(spec.key().value()).orElse(null);
        verifyRevision(spec.key().value(), current, command.expectedRevision());

        PreparedConfig<?> prepared = prepare(spec, command.value(), current);
        requireValid(spec, prepared);
        Long updatedBy = currentUserId();
        Instant updatedAt = HighDate.realInstant();
        ConfigValue savedValue;
        if (current == null) {
            savedValue = ConfigValue.createOverride(spec.key().value(), prepared.content(), spec.schemaVersion(),
                    updatedBy, updatedAt, command.reason());
            try {
                configValueRepository.insert(savedValue);
            } catch (DataIntegrityViolationException ex) {
                throw versionConflict(spec.key().value());
            }
        } else {
            savedValue = current.updateOverride(prepared.content(), spec.schemaVersion(), updatedBy, updatedAt,
                    command.reason());
            if (!configValueRepository.update(savedValue, command.expectedRevision())) {
                throw versionConflict(spec.key().value());
            }
        }

        refreshAfterCommit(spec, savedValue);
        return new ConfigChangeView(spec.key().value(), savedValue.revision(),
                spec.activationPolicy() == ConfigActivationPolicy.RESTART_REQUIRED);
    }

    @Transactional
    public ConfigChangeView resetDefault(ResetConfigCommand command) {
        if (command.expectedRevision() < 0) {
            throw new BizException("Invalid config reset command", BaseError.INVALID_PARAMETER);
        }
        ConfigSpec<?> spec = requireEditableSpec(command.configKey());
        ConfigValue current = configValueRepository.findByConfigKey(spec.key().value()).orElse(null);
        verifyRevision(spec.key().value(), current, command.expectedRevision());
        if (current == null) {
            return new ConfigChangeView(spec.key().value(), 0, false);
        }

        ConfigValue resetValue = current.resetToDefault(ConfigJsonCodec.serialize(spec.defaultValue()),
                spec.schemaVersion(), currentUserId(), HighDate.realInstant(), command.reason());
        if (!configValueRepository.update(resetValue, command.expectedRevision())) {
            throw versionConflict(spec.key().value());
        }

        refreshAfterCommit(spec, resetValue);
        return new ConfigChangeView(spec.key().value(), resetValue.revision(),
                spec.activationPolicy() == ConfigActivationPolicy.RESTART_REQUIRED);
    }

    private <T> PreparedConfig<T> prepare(ConfigSpec<T> spec, JsonNode submitted, ConfigValue current) {
        if (submitted == null || !submitted.isObject()) {
            return PreparedConfig.invalid(
                    new ConfigViolation("", "INVALID_OBJECT", "Config value must be a JSON object"));
        }

        CurrentTree currentTree = currentTree(spec, current);
        ConfigValueTreeSupport.MergeResult merged = ConfigValueTreeSupport.mergeSensitive(submitted,
                currentTree.value(), currentTree.readable(), spec.fields());
        var violations = new ArrayList<>(merged.violations());
        T typedValue;
        try {
            typedValue = ConfigJsonCodec.treeToValue(merged.value(), spec.valueClass());
        } catch (RuntimeException ex) {
            violations.add(new ConfigViolation("", "INVALID_TYPE", "Config value has an invalid structure"));
            return new PreparedConfig<>(null, "", violations);
        }

        addViolations(violations, spec.validateRuntime(typedValue));
        addViolations(violations, spec.validatePublish(typedValue));
        return new PreparedConfig<>(typedValue, ConfigJsonCodec.serialize(typedValue), violations);
    }

    private <T> CurrentTree currentTree(ConfigSpec<T> spec, ConfigValue current) {
        if (current == null || !current.configured()) {
            return new CurrentTree(ConfigJsonCodec.valueToTree(spec.defaultValue()), true);
        }
        try {
            JsonNode currentValue = ConfigJsonCodec.readTree(current.content());
            return new CurrentTree(currentValue, currentValue.isObject());
        } catch (RuntimeException ex) {
            return new CurrentTree(ConfigJsonCodec.valueToTree(spec.defaultValue()), false);
        }
    }

    private void addViolations(List<ConfigViolation> target, List<ConfigViolation> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private void requireValid(ConfigSpec<?> spec, PreparedConfig<?> prepared) {
        if (!prepared.violations().isEmpty()) {
            throw new ConfigValidationException("Config publish validation failed: " + spec.key(),
                    prepared.violations());
        }
    }

    private void verifyRevision(String configKey, ConfigValue current, long expectedRevision) {
        long actualRevision = current == null ? 0 : current.revision();
        if (actualRevision != expectedRevision) {
            throw versionConflict(configKey);
        }
    }

    private ConfigSpec<?> requireEditableSpec(String configKey) {
        ConfigSpec<?> spec = ConfigSpecCatalog.find(configKey)
                .orElseThrow(() -> new BizException("Config definition not found: " + configKey, BaseError.NOT_FOUND));
        if (spec.editPolicy() != ConfigEditPolicy.ADMIN_EDITABLE) {
            throw new BizException("Config is read-only: " + configKey, BaseError.FORBIDDEN);
        }
        return spec;
    }

    private Long currentUserId() {
        return securityContextService.current().userId();
    }

    private <T> void refreshAfterCommit(ConfigSpec<T> spec, ConfigValue savedValue) {
        if (spec.activationPolicy() != ConfigActivationPolicy.DYNAMIC) {
            return;
        }
        ConfigSnapshot<T> snapshot = ConfigSnapshotFactory.createStored(spec, savedValue.content(),
                savedValue.schemaVersion(), savedValue.revision(), savedValue.configured());
        afterCommit(() -> ConfigRegistry.replaceIfNewer(snapshot));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private ConfigVersionConflictException versionConflict(String configKey) {
        return new ConfigVersionConflictException(configKey);
    }

    private record PreparedConfig<T>(
            T value,
            String content,
            List<ConfigViolation> violations
    ) {

        private PreparedConfig {
            violations = List.copyOf(violations);
        }

        private static <T> PreparedConfig<T> invalid(ConfigViolation violation) {
            return new PreparedConfig<>(null, "", List.of(violation));
        }
    }

    private record CurrentTree(
            JsonNode value,
            boolean readable
    ) {
    }
}
