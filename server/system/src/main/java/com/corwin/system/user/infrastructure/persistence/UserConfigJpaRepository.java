package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link UserConfig} entity.
 * Provides auto-implemented query methods for user configuration lookups.
 *
 * @author Corwin 2026/3/30
 */
public interface UserConfigJpaRepository extends JpaRepository<UserConfig, Long> {

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
