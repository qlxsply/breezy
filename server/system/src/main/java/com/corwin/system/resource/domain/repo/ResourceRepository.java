package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Resource} entity operations.
 *
 * @author Corwin 2026/6/29
 */
public interface ResourceRepository extends DomainRepository<Resource, Long> {

  /**
   * Returns all resources.
   *
   * @return the list of all resources
   */
  List<Resource> findAll();

  /**
   * Returns all resources whose IDs are in the given collection.
   *
   * @param ids the collection of resource IDs
   * @return the matching resources
   */
  List<Resource> findAllById(Collection<Long> ids);

  /**
   * Finds a resource by its unique code.
   *
   * @param code the resource code
   * @return an Optional containing the resource if found
   */
  Optional<Resource> findByCode(String code);

  /**
   * Checks whether a resource with the given code exists.
   *
   * @param code the resource code
   * @return true if a resource with the given code exists
   */
  boolean existsByCode(String code);
}
