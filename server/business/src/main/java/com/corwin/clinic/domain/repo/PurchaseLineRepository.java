package com.corwin.clinic.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.clinic.domain.model.PurchaseLine;

import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public interface PurchaseLineRepository extends DomainRepository<PurchaseLine, Long> {
    List<PurchaseLine> findByPurchaseId(Long purchaseId);

    void deleteByPurchaseId(Long purchaseId);
}
