package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link UserRole} entity.
 * Provides auto-implemented query and delete methods by user ID or role ID.
 *
 * @author Corwin 2026/3/30
 */
public interface UserRoleJpaRepository extends JpaRepository<UserRole, Long> {

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
