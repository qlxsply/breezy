package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface PurchaseJpaRepository extends JpaRepository<Purchase, Long> {
    @Query("""
            select p from Purchase p
            where (:supplierId is null or p.supplierId = :supplierId)
              and (:startAt is null or p.purchasedAt >= :startAt)
              and (:endExclusive is null or p.purchasedAt < :endExclusive)
            """)
    Page<Purchase> page(@Param("supplierId") Long supplierId,
            @Param("startAt") LocalDate startAt,
            @Param("endExclusive") LocalDate endExclusive,
            Pageable pageable);
}
