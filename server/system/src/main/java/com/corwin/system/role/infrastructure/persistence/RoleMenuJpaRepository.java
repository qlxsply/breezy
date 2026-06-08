package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/19
 */
public interface RoleMenuJpaRepository extends JpaRepository<RoleMenu, Long> {

    List<RoleMenu> findByRoleId(Long roleId);

    List<RoleMenu> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
