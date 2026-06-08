package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface RoleJpaRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    List<Role> findByIdIn(List<Long> ids);
}
