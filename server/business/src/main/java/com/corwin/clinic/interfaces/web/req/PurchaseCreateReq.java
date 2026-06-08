package com.corwin.clinic.interfaces.web.req;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseCreateReq(
        Long supplierId,
        String supplierName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String remark,
        List<PurchaseLineReq> lines
) {
}
