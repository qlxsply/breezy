package com.corwin.system.config.domain.repo;

import com.corwin.system.config.domain.model.ConfigValue;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigValueRepository {

    Optional<ConfigValue> findByConfigKey(String configKey);

    List<ConfigValue> findAll();

    void insert(ConfigValue configValue);

    boolean update(ConfigValue configValue, long expectedRevision);
}
