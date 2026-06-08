package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface SaleJpaRepository extends JpaRepository<Sale, Long> {
    @Query("""
            select s from Sale s
            where (:customerNameLike is null or lower(s.customerName) like lower(concat('%', :customerNameLike, '%')))
              and (:startAt is null or s.soldAt >= :startAt)
              and (:endExclusive is null or s.soldAt < :endExclusive)
            """)
    Page<Sale> page(@Param("customerNameLike") String customerNameLike,
            @Param("startAt") LocalDate startAt,
            @Param("endExclusive") LocalDate endExclusive,
            Pageable pageable);
}
