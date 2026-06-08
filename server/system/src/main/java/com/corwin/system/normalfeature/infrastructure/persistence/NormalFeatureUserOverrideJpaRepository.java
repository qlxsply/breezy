package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureUserOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public interface NormalFeatureUserOverrideJpaRepository extends JpaRepository<NormalFeatureUserOverride, Long> {

    List<NormalFeatureUserOverride> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
