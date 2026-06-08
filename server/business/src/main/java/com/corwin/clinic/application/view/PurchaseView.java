package com.corwin.clinic.application.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseView(
        Long id,
        Long supplierId,
        String supplierName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String remark
) {
}
