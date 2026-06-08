package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseLineRes(
        Long id,
        Long skuId,
        String skuDisplayName,
        String manufacturer,
        String spec,
        BigDecimal qty,
        String unit,
        BigDecimal lineTotalAmount,
        BigDecimal unitPrice,
        String remark
) {
}
