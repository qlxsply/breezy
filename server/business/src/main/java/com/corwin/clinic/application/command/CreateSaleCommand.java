package com.corwin.clinic.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
public record CreateSaleCommand(
        LocalDate soldAt,
        BigDecimal totalAmount,
        String customerName,
        String remark,
        List<SaleLineCommand> lines
) {
}
