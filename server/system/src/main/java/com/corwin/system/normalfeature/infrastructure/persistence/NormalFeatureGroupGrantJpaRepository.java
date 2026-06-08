package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupGrantJpaRepository extends JpaRepository<NormalFeatureGroupGrant, Long> {

    List<NormalFeatureGroupGrant> findByGroupId(Long groupId);

    List<NormalFeatureGroupGrant> findByGroupIdIn(Iterable<Long> groupIds);

    void deleteByGroupId(Long groupId);
}
