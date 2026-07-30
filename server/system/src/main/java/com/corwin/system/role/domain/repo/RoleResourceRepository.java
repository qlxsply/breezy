package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.RoleResource;

import java.util.List;
import java.util.Collection;

/**
 * Repository interface for {@link RoleResource} domain aggregate.
 *
 * @author Corwin 2026/6/29
 */
public interface RoleResourceRepository extends DomainRepository<RoleResource, Long> {

    /**
     * Finds all resource associations for a given role.
     *
     * @param roleId the role ID
     * @return list of role-resource associations
     */
    List<RoleResource> findByRoleId(Long roleId);

    /**
     * Finds all resource associations for the given role IDs.
     *
     * @param roleIds list of role IDs
     * @return list of role-resource associations
     */
    List<RoleResource> findByRoleIdIn(List<Long> roleIds);

    /**
     * Finds all role associations for the given resource IDs.
     *
     * @param resourceIds collection of resource IDs
     * @return list of role-resource associations
     */
    List<RoleResource> findByResourceIdIn(Collection<Long> resourceIds);

    /**
     * Deletes all resource associations for a given role.
     *
     * @param roleId the role ID
     */
    void deleteByRoleId(Long roleId);

    /**
     * Deletes all role associations for the given resource IDs.
     *
     * @param resourceIds collection of resource IDs
     */
    void deleteByResourceIdIn(Collection<Long> resourceIds);
}
