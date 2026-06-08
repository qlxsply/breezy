package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.PurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public interface PurchaseLineJpaRepository extends JpaRepository<PurchaseLine, Long> {
    List<PurchaseLine> findByPurchaseId(Long purchaseId);

    void deleteByPurchaseId(Long purchaseId);
}
