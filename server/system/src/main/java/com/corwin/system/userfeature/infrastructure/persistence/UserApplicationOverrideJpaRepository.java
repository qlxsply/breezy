package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserApplicationOverride;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link UserApplicationOverride} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserApplicationOverrideJpaRepository
    extends JpaRepository<UserApplicationOverride, Long> {

  List<UserApplicationOverride> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
