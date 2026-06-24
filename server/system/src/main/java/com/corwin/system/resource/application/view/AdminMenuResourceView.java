package com.corwin.system.resource.application.view;

import java.util.List;

/**
 * @author Corwin 2026/5/31
 */
public record AdminMenuResourceView(
        String id,
        String parentId,
        String name,
        String icon,
        String code,
        String type,
        String url,
        String loadTarget,
        int orderNo,
        List<AdminMenuResourceView> children
) {
}
