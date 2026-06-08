package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseRes(
        Long id,
        Long supplierId,
        String supplierName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String remark
) {
}
