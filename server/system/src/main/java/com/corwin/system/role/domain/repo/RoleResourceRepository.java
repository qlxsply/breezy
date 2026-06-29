package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.RoleResource;

import java.util.List;
import java.util.Collection;

/**
 * @author Corwin 2026/6/29
 */
public interface RoleResourceRepository extends DomainRepository<RoleResource, Long> {

    List<RoleResource> findByRoleId(Long roleId);

    List<RoleResource> findByRoleIdIn(List<Long> roleIds);

    List<RoleResource> findByResourceIdIn(Collection<Long> resourceIds);

    void deleteByRoleId(Long roleId);

    void deleteByResourceIdIn(Collection<Long> resourceIds);
}
