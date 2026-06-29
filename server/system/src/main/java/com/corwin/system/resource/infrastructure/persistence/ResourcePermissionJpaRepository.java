package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.ResourcePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface ResourcePermissionJpaRepository extends JpaRepository<ResourcePermission, Long> {

    List<ResourcePermission> findByResourceIdIn(Collection<Long> resourceIds);
}
