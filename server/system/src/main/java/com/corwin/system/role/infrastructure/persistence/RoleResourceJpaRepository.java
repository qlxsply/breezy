package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleResource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RoleResource} entity.
 *
 * @author Corwin 2026/6/29
 */
public interface RoleResourceJpaRepository extends JpaRepository<RoleResource, Long> {

  /**
   * Finds all associations for a given role.
   *
   * @param roleId the role ID
   * @return list of role-resource associations
   */
  List<RoleResource> findByRoleId(Long roleId);

  /**
   * Finds all associations for the given role IDs.
   *
   * @param roleIds list of role IDs
   * @return list of role-resource associations
   */
  List<RoleResource> findByRoleIdIn(List<Long> roleIds);

  /**
   * Finds all associations for the given resource IDs.
   *
   * @param resourceIds collection of resource IDs
   * @return list of role-resource associations
   */
  List<RoleResource> findByResourceIdIn(Collection<Long> resourceIds);

  /**
   * Deletes all associations for a given role.
   *
   * @param roleId the role ID
   */
  void deleteByRoleId(Long roleId);

  /**
   * Deletes all associations for the given resource IDs.
   *
   * @param resourceIds collection of resource IDs
   */
  void deleteByResourceIdIn(Collection<Long> resourceIds);
}
