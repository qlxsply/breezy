package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.UserConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for the {@link UserConfig} entity.
 * Provides query methods for user-specific configuration lookups.
 *
 * @author Corwin 2026/3/30
 */
public interface UserConfigRepository extends DomainRepository<UserConfig, Long> {

    /**
     * Returns all user configuration entries.
     *
     * @return a list of all UserConfig records
     */
    List<UserConfig> findAll();

    /**
     * Finds all configuration entries for a given user.
     *
     * @param userId the user ID
     * @return a list of UserConfig entries
     */
    List<UserConfig> findByUserId(Long userId);

    /**
     * Finds a specific configuration entry for a user by config code.
     *
     * @param userId     the user ID
     * @param configCode the configuration code
     * @return an Optional containing the config entry if found
     */
    Optional<UserConfig> findByUserIdAndConfigCode(Long userId, String configCode);

    /**
     * Finds all configuration entries with a given config code across users.
     *
     * @param configCode the configuration code
     * @return a list of UserConfig entries
     */
    List<UserConfig> findByConfigCode(String configCode);

    /**
     * Finds configuration entries by code and value.
     *
     * @param configCode  the configuration code
     * @param configValue the configuration value
     * @return a list of matching UserConfig entries
     */
    List<UserConfig> findByConfigCodeAndConfigValue(String configCode, String configValue);
}
