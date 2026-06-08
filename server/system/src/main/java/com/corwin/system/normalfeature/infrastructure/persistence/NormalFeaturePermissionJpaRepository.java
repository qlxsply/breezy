package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public interface NormalFeaturePermissionJpaRepository extends JpaRepository<NormalFeaturePermission, Long> {

    List<NormalFeaturePermission> findByFeatureIdIn(Iterable<Long> featureIds);
}
