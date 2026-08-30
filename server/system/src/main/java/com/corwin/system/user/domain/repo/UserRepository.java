package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link User} domain aggregate. Provides query methods for user lookup,
 * existence checks, and pagination.
 *
 * @author Corwin 2026/1/22
 */
public interface UserRepository extends DomainRepository<User, Long> {

  /**
   * Finds a user by their username.
   *
   * @param username the username to search for
   * @return an Optional containing the user if found
   */
  Optional<User> findByUsername(String username);

  /**
   * Checks whether a username is already taken.
   *
   * @param username the username to check
   * @return true if a user with the given username exists
   */
  boolean existsByUsername(String username);

  /**
   * Returns all users ordered by ID ascending.
   *
   * @return a list of all users
   */
  List<User> findAllByOrderByIdAsc();

  /**
   * Paginates users filtered by status and/or username (LIKE match).
   *
   * @param status optional status filter
   * @param usernameLike optional username fuzzy match pattern
   * @param spec the pagination specification
   * @return a page of users
   */
  PageData<User> page(UserStatus status, String usernameLike, PageSpec spec);
}
