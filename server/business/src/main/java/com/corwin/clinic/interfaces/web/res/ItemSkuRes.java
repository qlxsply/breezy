package com.corwin.clinic.interfaces.web.res;

import com.corwin.clinic.domain.model.ItemCategory;

/**
 * @author Corwin 2026/2/8
 */
public record ItemSkuRes(
        Long id,
        Long spuId,
        ItemCategory category,
        String displayName,
        String manufacturer,
        String spec,
        boolean enabled
) {
}
