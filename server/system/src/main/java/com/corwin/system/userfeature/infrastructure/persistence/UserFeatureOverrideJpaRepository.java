package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserFeatureOverride;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link UserFeatureOverride} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserFeatureOverrideJpaRepository extends JpaRepository<UserFeatureOverride, Long> {

  List<UserFeatureOverride> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
