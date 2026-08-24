package com.corwin.system.resource.application.view;

import java.util.List;

/**
 * View object representing a single node in the admin menu resource tree.
 *
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
        int orderNo,
        List<AdminMenuResourceView> children
) {
}
