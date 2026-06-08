package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeature;

import java.util.List;

/**
 * @author Corwin 2026/4/20
 */
public interface NormalFeatureRepository extends DomainRepository<NormalFeature, Long> {

    List<NormalFeature> findAll();

    List<NormalFeature> findByIdIn(Iterable<Long> ids);

    void deleteAll();
}
