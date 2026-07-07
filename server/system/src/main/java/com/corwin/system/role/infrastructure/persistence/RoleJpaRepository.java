package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface RoleJpaRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    Page<Role> findByEnabled(boolean enabled, Pageable pageable);

    Page<Role> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name, Pageable pageable);

    Page<Role> findByEnabledAndCodeContainingIgnoreCaseOrEnabledAndNameContainingIgnoreCase(boolean enabled1, String code,
            boolean enabled2, String name, Pageable pageable);

    Page<Role> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Role> findByEnabledAndCodeContainingIgnoreCase(boolean enabled, String code, Pageable pageable);

    Page<Role> findByEnabledAndNameContainingIgnoreCase(boolean enabled, String name, Pageable pageable);

    List<Role> findByIdIn(List<Long> ids);
}
