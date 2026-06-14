package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserApplicationOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface UserApplicationOverrideJpaRepository extends JpaRepository<UserApplicationOverride, Long> {

    List<UserApplicationOverride> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
