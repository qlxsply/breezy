package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeatureUserOverride;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public interface NormalFeatureUserOverrideRepository extends DomainRepository<NormalFeatureUserOverride, Long> {

    List<NormalFeatureUserOverride> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
