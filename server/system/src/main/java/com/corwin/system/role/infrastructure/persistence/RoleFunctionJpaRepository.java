package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleFunction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface RoleFunctionJpaRepository extends JpaRepository<RoleFunction, Long> {

    List<RoleFunction> findByRoleId(Long roleId);

    List<RoleFunction> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
