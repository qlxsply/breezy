package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface RoleResourceJpaRepository extends JpaRepository<RoleResource, Long> {

    List<RoleResource> findByRoleId(Long roleId);

    List<RoleResource> findByRoleIdIn(List<Long> roleIds);

    List<RoleResource> findByResourceIdIn(Collection<Long> resourceIds);

    void deleteByRoleId(Long roleId);

    void deleteByResourceIdIn(Collection<Long> resourceIds);
}
