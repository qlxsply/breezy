package com.corwin.clinic.domain.repo;

import com.corwin.clinic.domain.model.Supplier;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface SupplierRepository extends DomainRepository<Supplier, Long> {
    Optional<Supplier> findByNameNorm(String nameNorm);

    List<Supplier> findByIdIn(List<Long> ids);

    PageData<Supplier> page(String nameLike, Boolean enabled, PageSpec spec);
}
