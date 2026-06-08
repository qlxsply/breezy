package com.corwin.system.config.infrastructure.persistence;

import com.corwin.system.config.domain.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/5
 */
public interface ConfigJpaRepository extends JpaRepository<Config, String> {

    List<Config> findAllByExpiredFalseOrderByCodeAsc();

    Optional<Config> findByCodeAndExpiredFalse(String code);
}
