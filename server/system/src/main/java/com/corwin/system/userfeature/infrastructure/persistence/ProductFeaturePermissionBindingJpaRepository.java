package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductFeaturePermissionBinding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link ProductFeaturePermissionBinding} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface ProductFeaturePermissionBindingJpaRepository
    extends JpaRepository<ProductFeaturePermissionBinding, Long> {

  List<ProductFeaturePermissionBinding> findByFeatureIdIn(Iterable<Long> featureIds);
}
