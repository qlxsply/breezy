package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/4/20
 */
public interface NormalFeatureJpaRepository extends JpaRepository<NormalFeature, Long> {

    List<NormalFeature> findByIdIn(Iterable<Long> ids);
}
