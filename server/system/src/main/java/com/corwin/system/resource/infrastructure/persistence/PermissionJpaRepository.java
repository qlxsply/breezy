package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Permission} entity.
 *
 * <p>Provides database access for permission code CRUD operations.
 *
 * @author Corwin 2026/4/19
 */
public interface PermissionJpaRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByCode(String code);

  boolean existsByCode(String code);
}
