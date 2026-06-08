package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.RoleMenu;

import java.util.List;

/**
 * @author Corwin 2026/5/19
 */
public interface RoleMenuRepository extends DomainRepository<RoleMenu, Long> {

    List<RoleMenu> findByRoleId(Long roleId);

    List<RoleMenu> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
