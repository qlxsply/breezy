package com.corwin.clinic.interfaces.web.res;

/**
 * @author Corwin 2026/2/8
 */
public record SupplierRes(
        Long id,
        String name,
        boolean enabled,
        String remark
) {
}
