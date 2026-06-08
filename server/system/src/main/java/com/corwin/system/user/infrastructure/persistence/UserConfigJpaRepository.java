package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface UserConfigJpaRepository extends JpaRepository<UserConfig, Long> {

    List<UserConfig> findByUserId(Long userId);

    Optional<UserConfig> findByUserIdAndConfigCode(Long userId, String configCode);

    List<UserConfig> findByConfigCode(String configCode);

    List<UserConfig> findByConfigCodeAndConfigValue(String configCode, String configValue);
}
