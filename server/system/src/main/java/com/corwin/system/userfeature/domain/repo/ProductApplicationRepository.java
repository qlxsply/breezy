package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.ProductApplication;
import java.util.List;

/**
 * Domain repository for product applications.
 *
 * @author Corwin 2026/6/14
 */
public interface ProductApplicationRepository extends DomainRepository<ProductApplication, Long> {

  PageData<ProductApplication> page(String keyword, Boolean enabled, PageSpec spec);

  List<ProductApplication> findAll();

  List<ProductApplication> findByIdIn(Iterable<Long> ids);
}
