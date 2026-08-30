package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Role} entity.
 *
 * @author Corwin 2026/3/30
 */
public interface RoleJpaRepository extends JpaRepository<Role, Long> {

  /**
   * Finds a role by its unique code.
   *
   * @param code the role code
   * @return an Optional containing the role
   */
  Optional<Role> findByCode(String code);

  /**
   * Checks whether a role with the given code exists.
   *
   * @param code the role code
   * @return true if exists
   */
  boolean existsByCode(String code);

  /**
   * Finds roles by enabled status with pagination.
   *
   * @param enabled the enabled status
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByEnabled(boolean enabled, Pageable pageable);

  /**
   * Finds roles where code or name contains the given keyword (case-insensitive).
   *
   * @param code keyword for code search
   * @param name keyword for name search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
      String code, String name, Pageable pageable);

  /**
   * Finds enabled roles where code or name contains the given keyword (case-insensitive).
   *
   * @param enabled1 enabled filter (paired with code)
   * @param code keyword for code search
   * @param enabled2 enabled filter (paired with name)
   * @param name keyword for name search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByEnabledAndCodeContainingIgnoreCaseOrEnabledAndNameContainingIgnoreCase(
      boolean enabled1, String code, boolean enabled2, String name, Pageable pageable);

  /**
   * Finds roles where code contains the given keyword (case-insensitive).
   *
   * @param code keyword for code search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByCodeContainingIgnoreCase(String code, Pageable pageable);

  /**
   * Finds roles where name contains the given keyword (case-insensitive).
   *
   * @param name keyword for name search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /**
   * Finds enabled roles where code contains the given keyword (case-insensitive).
   *
   * @param enabled enabled status
   * @param code keyword for code search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByEnabledAndCodeContainingIgnoreCase(
      boolean enabled, String code, Pageable pageable);

  /**
   * Finds enabled roles where name contains the given keyword (case-insensitive).
   *
   * @param enabled enabled status
   * @param name keyword for name search
   * @param pageable pagination parameters
   * @return paginated results
   */
  Page<Role> findByEnabledAndNameContainingIgnoreCase(
      boolean enabled, String name, Pageable pageable);

  /**
   * Finds roles by a list of IDs.
   *
   * @param ids list of role IDs
   * @return matching roles
   */
  List<Role> findByIdIn(List<Long> ids);
}
