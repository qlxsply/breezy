package com.corwin.system.config.infrastructure.persistence;

import com.corwin.framework.config.ConfigDefinitionCatalog;
import com.corwin.framework.config.ConfigDefinitionDescriptor;
import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.config.ConfigStore;
import com.corwin.framework.config.StoredConfig;
import com.corwin.system.config.domain.model.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JPA-based implementation of ConfigStore.
 * Retrieves and persists system configuration using JPA repository,
 * merges stored entities with registered definitions, and refreshes
 * the ConfigRegistry on value updates.
 *
 * @author Corwin 2026/5/5
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaConfigStore implements ConfigStore {

    private final ConfigJpaRepository configJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<StoredConfig> findAllActive() {
        return mergeAllDefinitions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<StoredConfig> findByKeyword(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return findAllActive();
        }
        String normalizedKeyword = trimmed.toLowerCase(Locale.ROOT);
        return mergeAllDefinitions().stream()
                .filter(item -> item.code().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.description().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<StoredConfig> findByLevel(ConfigLevel level) {
        return mergeAllDefinitions().stream().filter(item -> item.level() == level).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<StoredConfig> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return configJpaRepository.findByCodeAndExpiredFalse(normalized)
                .map(this::toStoredConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean updateValue(String code, String value) {
        Optional<Config> optional = configJpaRepository.findByCodeAndExpiredFalse(code);
        if (optional.isEmpty()) {
            return false;
        }

        Config entity = optional.get();
        entity.updateValue(value);
        configJpaRepository.save(entity);

        ConfigRegistry.refresh(toStoredConfig(entity).toItem());
        log.info("Refreshed ConfigRegistry after updating sys_config.{}", code);
        return true;
    }

    private List<StoredConfig> mergeAllDefinitions() {
        return configJpaRepository.findAllByExpiredFalseOrderByCodeAsc().stream()
                .map(this::toStoredConfig)
                .toList();
    }
    private StoredConfig toStoredConfig(Config entity) {
        ConfigDefinitionDescriptor definition = ConfigDefinitionCatalog.findByCode(entity.getCode()).orElse(null);
        return new StoredConfig(entity.getCode(), entity.getDescription(), entity.getConfigValueType(),
                entity.getConfigValue(), entity.getLevel(), definition == null ? null : definition.scope());
    }
}
