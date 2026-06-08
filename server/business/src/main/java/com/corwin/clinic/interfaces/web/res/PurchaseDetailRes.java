package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseDetailRes(
        Long id,
        Long supplierId,
        String supplierName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String remark,
        List<PurchaseLineRes> lines
) {
}
