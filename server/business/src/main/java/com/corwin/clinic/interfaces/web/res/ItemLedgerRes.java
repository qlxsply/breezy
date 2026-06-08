package com.corwin.clinic.interfaces.web.res;

import com.corwin.clinic.domain.model.LedgerBizType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public record ItemLedgerRes(
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
