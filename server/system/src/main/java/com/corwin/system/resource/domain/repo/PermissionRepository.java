package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Permission} entity operations.
 *
 * @author Corwin 2026/4/19
 */
public interface PermissionRepository extends DomainRepository<Permission, Long> {

    /**
     * Finds a permission by its unique code.
     *
     * @param code the permission code
     * @return an Optional containing the permission if found
     */
    Optional<Permission> findByCode(String code);

    /**
     * Checks whether a permission with the given code exists.
     *
     * @param code the permission code
     * @return true if a permission with the given code exists
     */
    boolean existsByCode(String code);

    /**
     * Returns all permissions.
     *
     * @return the list of all permissions
     */
    List<Permission> findAll();

    /**
     * Returns all permissions whose IDs are in the given iterable.
     *
     * @param ids the iterable of permission IDs
     * @return the matching permissions
     */
    List<Permission> findAllById(Iterable<Long> ids);
}
