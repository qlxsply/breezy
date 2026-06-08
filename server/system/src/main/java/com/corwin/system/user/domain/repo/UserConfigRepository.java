package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.UserConfig;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface UserConfigRepository extends DomainRepository<UserConfig, Long> {

    List<UserConfig> findAll();

    List<UserConfig> findByUserId(Long userId);

    Optional<UserConfig> findByUserIdAndConfigCode(Long userId, String configCode);

    List<UserConfig> findByConfigCode(String configCode);

    List<UserConfig> findByConfigCodeAndConfigValue(String configCode, String configValue);
}
