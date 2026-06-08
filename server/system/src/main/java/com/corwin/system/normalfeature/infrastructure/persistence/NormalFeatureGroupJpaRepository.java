package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupJpaRepository extends JpaRepository<NormalFeatureGroup, Long> {

    Optional<NormalFeatureGroup> findByCode(String code);

    boolean existsByCode(String code);

    List<NormalFeatureGroup> findByDefaultGroupTrueAndEnabledTrue();

    List<NormalFeatureGroup> findByIdIn(Iterable<Long> ids);

    Page<NormalFeatureGroup> findByEnabled(Boolean enabled, Pageable pageable);

    Page<NormalFeatureGroup> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name,
            Pageable pageable);

    Page<NormalFeatureGroup> findByEnabledAndCodeContainingIgnoreCaseOrEnabledAndNameContainingIgnoreCase(
            Boolean leftEnabled, String code, Boolean rightEnabled, String name, Pageable pageable);
}
