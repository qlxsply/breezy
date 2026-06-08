package com.corwin.clinic.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.clinic.domain.model.SaleLine;

import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public interface SaleLineRepository extends DomainRepository<SaleLine, Long> {
    List<SaleLine> findBySaleId(Long saleId);

    void deleteBySaleId(Long saleId);
}
