package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record SaleDetailRes(
        Long id,
        LocalDate soldAt,
        BigDecimal totalAmount,
        BigDecimal linesTotalAmount,
        BigDecimal diffAmount,
        String customerName,
        String remark,
        List<SaleLineRes> lines
) {
}
