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
}
