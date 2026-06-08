package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
public interface PermissionJpaRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);
}
