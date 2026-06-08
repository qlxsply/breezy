package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.ItemLedger;
import com.corwin.clinic.domain.model.LedgerBizType;
import com.corwin.clinic.domain.repo.SkuInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemLedgerJpaRepository extends JpaRepository<ItemLedger, Long> {
    @Query("""
            select l from ItemLedger l
            where (:skuId is null or l.skuId = :skuId)
              and (:bizType is null or l.bizType = :bizType)
              and (:supplierId is null or l.supplierId = :supplierId)
              and (:startAt is null or l.occurredAt >= :startAt)
              and (:endExclusive is null or l.occurredAt < :endExclusive)
            """)
    Page<ItemLedger> page(@Param("skuId") Long skuId, @Param("bizType") LedgerBizType bizType,
            @Param("supplierId") Long supplierId, @Param("startAt") LocalDate startAt,
            @Param("endExclusive") LocalDate endExclusive, Pageable pageable);

    void deleteByBizTypeAndBizId(LedgerBizType bizType, Long bizId);

    @Query("""
            select new com.corwin.clinic.domain.repo.SkuInventory(
                l.skuId,
                sum(case when l.bizType = com.corwin.clinic.domain.model.LedgerBizType.PURCHASE then l.qty else -l.qty end)
            )
            from ItemLedger l
            where (:skuId is null or l.skuId = :skuId)
            group by l.skuId
            """)
    Page<SkuInventory> pageInventory(@Param("skuId") Long skuId, Pageable pageable);
}

