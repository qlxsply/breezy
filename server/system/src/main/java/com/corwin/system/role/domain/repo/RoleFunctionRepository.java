package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.RoleFunction;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface RoleFunctionRepository extends DomainRepository<RoleFunction, Long> {

    List<RoleFunction> findByRoleId(Long roleId);

    List<RoleFunction> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
