package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.UserRole;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
public interface UserRoleRepository extends DomainRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);
}
