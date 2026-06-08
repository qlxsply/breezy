package com.corwin.clinic.application.view;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/9
 */
public record ItemInventoryView(
        Long skuId,
        String skuDisplayName,
        String manufacturer,
        String spec,
        BigDecimal inventoryQty
) {
}
