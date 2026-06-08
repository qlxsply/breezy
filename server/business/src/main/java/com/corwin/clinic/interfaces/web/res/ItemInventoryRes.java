package com.corwin.clinic.interfaces.web.res;

import java.math.BigDecimal;

/**
 * @author Corwin 2026/2/9
 */
public record ItemInventoryRes(
        Long skuId,
        String skuDisplayName,
        String manufacturer,
        String spec,
        BigDecimal inventoryQty
) {
}
