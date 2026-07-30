package com.corwin.system.user.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link User} entity.
 * Provides auto-implemented query methods for user lookups and pagination.
 *
 * @author Corwin 2026/3/30
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds the first user of a given user type.
     *
     * @param userType the user type
     * @return an Optional containing the user if found
     */
    Optional<User> findFirstByUserType(UserType userType);

    /**
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return true if a user with the given username exists
     */
    boolean existsByUsername(String username);

    /**
     * Paginates users filtered by status.
     *
     * @param status   the status filter
     * @param pageable the pagination parameters
     * @return a page of users
     */
    Page<User> findByUserStatus(UserStatus status, Pageable pageable);

    /**
     * Paginates users filtered by username (case-insensitive contains).
     *
     * @param username the username filter
     * @param pageable the pagination parameters
     * @return a page of users
     */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * Paginates users filtered by both status and username (case-insensitive contains).
     *
     * @param status   the status filter
     * @param username the username filter
     * @param pageable the pagination parameters
     * @return a page of users
     */
    Page<User> findByUserStatusAndUsernameContainingIgnoreCase(UserStatus status, String username, Pageable pageable);
}
