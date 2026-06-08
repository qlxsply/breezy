package com.corwin.clinic.domain.repo;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/9
 */
public record SkuInventory(
        Long skuId,
        BigDecimal inventoryQty
) {
}
