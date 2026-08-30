package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.ProductFeature;
import java.util.List;

/**
 * Domain repository for product features (application capabilities).
 *
 * @author Corwin 2026/6/14
 */
public interface ProductFeatureRepository extends DomainRepository<ProductFeature, Long> {

  List<ProductFeature> findAll();

  List<ProductFeature> findByIdIn(Iterable<Long> ids);

  List<ProductFeature> findByApplicationId(Long applicationId);

  List<ProductFeature> findByApplicationIdIn(Iterable<Long> applicationIds);
}
