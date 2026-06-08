package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
public interface NormalFeaturePermissionRepository extends DomainRepository<NormalFeaturePermission, Long> {

    List<NormalFeaturePermission> findAll();

    List<NormalFeaturePermission> findByFeatureIdIn(Iterable<Long> featureIds);
}
