package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link ProductApplication} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface ProductApplicationJpaRepository extends JpaRepository<ProductApplication, Long> {

  List<ProductApplication> findByIdIn(Iterable<Long> ids);
}
