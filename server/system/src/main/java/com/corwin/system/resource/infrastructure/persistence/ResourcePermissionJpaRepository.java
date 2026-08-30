package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.ResourcePermission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link ResourcePermission} entity.
 *
 * <p>Provides database access for resource-permission binding operations.
 *
 * @author Corwin 2026/6/29
 */
public interface ResourcePermissionJpaRepository extends JpaRepository<ResourcePermission, Long> {

  List<ResourcePermission> findByResourceIdIn(Collection<Long> resourceIds);

  void deleteByResourceIdIn(Collection<Long> resourceIds);
}
