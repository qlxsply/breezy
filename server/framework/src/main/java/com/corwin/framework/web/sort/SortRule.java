package com.corwin.framework.web.sort;

import com.corwin.framework.domain.page.SortSpec;

import java.util.List;

/**
 * 当前接口排序规则。
 *
 * @author Corwin 2026/7/29
 */
public record SortRule(
        boolean enabled,
        List<SortableField> allowed,
        List<SortSpec> defaults
) {

    public SortRule {
        allowed = allowed == null ? List.of() : List.copyOf(allowed);
        defaults = defaults == null ? List.of() : List.copyOf(defaults);
    }

    public static SortRule disabled() {
        return new SortRule(false, List.of(), List.of());
    }
}
