package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductFeaturePermissionBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface ProductFeaturePermissionBindingJpaRepository extends JpaRepository<ProductFeaturePermissionBinding, Long> {

    List<ProductFeaturePermissionBinding> findByFeatureIdIn(Iterable<Long> featureIds);
}
