package com.corwin.clinic.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.clinic.domain.model.Purchase;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface PurchaseRepository extends DomainRepository<Purchase, Long> {
    PageData<Purchase> page(Long supplierId, LocalDate startAt, LocalDate endAt, PageSpec spec);
}
