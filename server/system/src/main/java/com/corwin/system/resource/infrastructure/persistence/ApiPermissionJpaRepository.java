package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.ApiPermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link ApiPermission} entity.
 *
 * <p>Provides database access for API-permission binding operations.
 *
 * @author Corwin 2026/4/24
 */
public interface ApiPermissionJpaRepository extends JpaRepository<ApiPermission, Long> {

  List<ApiPermission> findByApiId(Long apiId);

  void deleteByApiId(Long apiId);
}
