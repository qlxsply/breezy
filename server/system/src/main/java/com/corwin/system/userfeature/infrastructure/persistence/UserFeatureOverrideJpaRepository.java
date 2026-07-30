package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link UserFeatureOverride} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserFeatureOverrideJpaRepository extends JpaRepository<UserFeatureOverride, Long> {

    List<UserFeatureOverride> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
