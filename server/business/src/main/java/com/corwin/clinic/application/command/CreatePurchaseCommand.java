package com.corwin.clinic.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record CreatePurchaseCommand(
        Long supplierId,
        String supplierName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String remark,
        List<PurchaseLineCommand> lines
) {
}
