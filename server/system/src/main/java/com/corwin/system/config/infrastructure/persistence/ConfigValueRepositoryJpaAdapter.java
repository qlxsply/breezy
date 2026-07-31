package com.corwin.system.config.infrastructure.persistence;

import com.corwin.system.config.domain.model.ConfigValue;
import com.corwin.system.config.domain.repo.ConfigValueRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/7/30
 */
@Repository
@RequiredArgsConstructor
public class ConfigValueRepositoryJpaAdapter implements ConfigValueRepository {

    private final ConfigValueJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<ConfigValue> findByConfigKey(String configKey) {
        return jpaRepository.findById(configKey).map(ConfigValueEntity::toDomain);
    }

    @Override
    public List<ConfigValue> findAll() {
        return jpaRepository.findAllByOrderByConfigKeyAsc().stream().map(ConfigValueEntity::toDomain).toList();
    }

    @Override
    public void insert(ConfigValue configValue) {
        entityManager.persist(ConfigValueEntity.fromDomain(configValue));
        entityManager.flush();
    }

    @Override
    public boolean update(ConfigValue configValue, long expectedRevision) {
        if (expectedRevision < 1 || configValue.revision() != Math.addExact(expectedRevision, 1)) {
            throw new IllegalArgumentException("configValue revision must immediately follow expectedRevision");
        }
        int updated = jpaRepository.updateByExpectedRevision(configValue.configKey(), configValue.content(),
                configValue.schemaVersion(), configValue.revision(), configValue.configured(), configValue.updatedBy(),
                configValue.updatedAt(), configValue.updateReason(), expectedRevision);
        return updated == 1;
    }
}
