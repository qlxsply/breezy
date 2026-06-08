package com.corwin.clinic.application.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record SaleDetailView(
        Long id,
        LocalDate soldAt,
        BigDecimal totalAmount,
        BigDecimal linesTotalAmount,
        BigDecimal diffAmount,
        String customerName,
        String remark,
        List<SaleLineView> lines
) {
}
