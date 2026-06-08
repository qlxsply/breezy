package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record SaleRes(
        Long id,
        LocalDate soldAt,
        BigDecimal totalAmount,
        String customerName,
        String remark
) {
}
