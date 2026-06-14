package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface ProductFeatureJpaRepository extends JpaRepository<ProductFeature, Long> {

    List<ProductFeature> findByIdIn(Iterable<Long> ids);

    List<ProductFeature> findByApplicationId(Long applicationId);

    List<ProductFeature> findByApplicationIdIn(Iterable<Long> applicationIds);
}
