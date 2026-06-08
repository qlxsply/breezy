package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public interface UserRoleJpaRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);
}
