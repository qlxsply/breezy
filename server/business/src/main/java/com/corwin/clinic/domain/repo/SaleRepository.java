package com.corwin.clinic.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.clinic.domain.model.Sale;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface SaleRepository extends DomainRepository<Sale, Long> {
    PageData<Sale> page(String customerNameLike, LocalDate startAt, LocalDate endAt, PageSpec spec);
}
