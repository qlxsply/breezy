package com.corwin.system.user.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findFirstByUserType(UserType userType);

    boolean existsByUsername(String username);

    Page<User> findByUserStatus(UserStatus status, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<User> findByUserStatusAndUsernameContainingIgnoreCase(UserStatus status, String username, Pageable pageable);
}
