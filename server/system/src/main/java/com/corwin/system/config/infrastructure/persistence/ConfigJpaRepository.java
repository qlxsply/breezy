package com.corwin.system.config.infrastructure.persistence;

import com.corwin.system.config.domain.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Config entity.
 * Provides data access for non-expired configuration entries.
 *
 * @author Corwin 2026/5/5
 */
public interface ConfigJpaRepository extends JpaRepository<Config, String> {

    /**
     * Find all non-expired configurations ordered by code ascending.
     *
     * @return list of non-expired configurations
     */
    List<Config> findAllByExpiredFalseOrderByCodeAsc();

    /**
     * Find a non-expired configuration by its code.
     *
     * @param code the configuration code
     * @return an Optional containing the configuration if found
     */
    Optional<Config> findByCodeAndExpiredFalse(String code);
}
