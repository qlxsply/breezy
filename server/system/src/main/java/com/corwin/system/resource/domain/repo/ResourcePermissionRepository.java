package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.ResourcePermission;
import java.util.Collection;
import java.util.List;

/**
 * Repository interface for {@link ResourcePermission} entity operations.
 *
 * @author Corwin 2026/6/29
 */
public interface ResourcePermissionRepository extends DomainRepository<ResourcePermission, Long> {

  /**
   * Returns all resource-permission bindings.
   *
   * @return the list of all bindings
   */
  List<ResourcePermission> findAll();

  /**
   * Finds all bindings whose resource ID is in the given collection.
   *
   * @param resourceIds the collection of resource IDs
   * @return the matching bindings
   */
  List<ResourcePermission> findByResourceIdIn(Collection<Long> resourceIds);

  /**
   * Deletes all bindings whose resource ID is in the given collection.
   *
   * @param resourceIds the collection of resource IDs
   */
  void deleteByResourceIdIn(Collection<Long> resourceIds);
}
