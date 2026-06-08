package com.corwin.clinic.application.view;

import com.corwin.clinic.domain.model.LedgerBizType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record ItemLedgerView(
        Long id,
        Long skuId,
        String skuDisplayName,
        LedgerBizType bizType,
        Long bizId,
        Long bizLineId,
        LocalDate occurredAt,
        BigDecimal qty,
        String unit,
        BigDecimal amountTotal,
        Long supplierId,
        String supplierName,
        String remark
) {
}
