package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.UserRole;

import java.util.List;

/**
 * Repository interface for the {@link UserRole} association entity.
 * Provides query and delete operations by user ID or role ID.
 *
 * @author Corwin 2026/1/23
 */
public interface UserRoleRepository extends DomainRepository<UserRole, Long> {

    /**
     * Finds all role assignments for a given user.
     *
     * @param userId the user ID
     * @return a list of UserRole associations
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * Finds all user assignments for a given role.
     *
     * @param roleId the role ID
     * @return a list of UserRole associations
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * Deletes all role assignments for a given user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(Long userId);

    /**
     * Deletes all user assignments for a given role.
     *
     * @param roleId the role ID
     */
    void deleteByRoleId(Long roleId);
}
