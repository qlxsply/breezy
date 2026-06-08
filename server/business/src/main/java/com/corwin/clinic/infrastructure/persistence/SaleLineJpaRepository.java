package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public interface SaleLineJpaRepository extends JpaRepository<SaleLine, Long> {
    List<SaleLine> findBySaleId(Long saleId);

    void deleteBySaleId(Long saleId);
}
