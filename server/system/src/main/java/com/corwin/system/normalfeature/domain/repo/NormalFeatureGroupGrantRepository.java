package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupGrant;

import java.util.List;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupGrantRepository extends DomainRepository<NormalFeatureGroupGrant, Long> {

    List<NormalFeatureGroupGrant> findByGroupId(Long groupId);

    List<NormalFeatureGroupGrant> findByGroupIdIn(Iterable<Long> groupIds);

    void deleteByGroupId(Long groupId);
}
