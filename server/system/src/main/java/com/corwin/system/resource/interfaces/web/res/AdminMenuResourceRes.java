package com.corwin.system.resource.interfaces.web.res;

import java.util.List;

/**
 * @author Corwin 2026/5/31
 */
public record AdminMenuResourceRes(
        String id,
        String parentId,
        String name,
        String icon,
        String code,
        String type,
        String url,
        String loadTarget,
        int orderNo,
        List<AdminMenuResourceRes> children
) {
}
