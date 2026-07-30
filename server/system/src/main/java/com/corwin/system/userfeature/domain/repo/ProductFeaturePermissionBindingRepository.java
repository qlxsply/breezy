package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.ProductFeaturePermissionBinding;

import java.util.List;

/**
 * Domain repository for feature-permission binding relations.
 *
 * @author Corwin 2026/6/14
 */
public interface ProductFeaturePermissionBindingRepository extends DomainRepository<ProductFeaturePermissionBinding, Long> {

    List<ProductFeaturePermissionBinding> findAll();

    List<ProductFeaturePermissionBinding> findByFeatureIdIn(Iterable<Long> featureIds);
}
