package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.RoleResource;

import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface RoleResourceRepository extends DomainRepository<RoleResource, Long> {

    List<RoleResource> findByRoleId(Long roleId);

    List<RoleResource> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
