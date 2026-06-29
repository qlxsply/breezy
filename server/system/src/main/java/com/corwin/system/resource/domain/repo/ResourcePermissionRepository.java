package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.ResourcePermission;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface ResourcePermissionRepository extends DomainRepository<ResourcePermission, Long> {

    List<ResourcePermission> findAll();

    List<ResourcePermission> findByResourceIdIn(Collection<Long> resourceIds);
}
