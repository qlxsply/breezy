package com.corwin.clinic.application.command;

import com.corwin.clinic.domain.model.ItemCategory;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/8
 */
public record PurchaseLineCommand(
        Long skuId,
        ItemCategory category,
        String itemName,
        String manufacturer,
        String spec,
        BigDecimal qty,
        String unit,
        BigDecimal lineTotalAmount,
        String remark
) {
}
