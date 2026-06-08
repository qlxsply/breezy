package com.corwin.clinic.application.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record SaleView(
        Long id,
        LocalDate soldAt,
        BigDecimal totalAmount,
        String customerName,
        String remark
) {
}
