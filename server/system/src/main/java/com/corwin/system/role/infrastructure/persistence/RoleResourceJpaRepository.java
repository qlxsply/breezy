package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface RoleResourceJpaRepository extends JpaRepository<RoleResource, Long> {

    List<RoleResource> findByRoleId(Long roleId);

    List<RoleResource> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
