package com.corwin.framework.web.request;

import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public final class PageSpecFactory {

    private PageSpecFactory() {
    }

    public static PageSpec of(PageRuleRequest page, SortRuleRequest sort) {
        Integer pageNo = page == null ? null : page.pageNo();
        Integer pageSize = page == null ? null : page.pageSize();
        return PageSpec.of(pageNo, pageSize, toSorts(sort));
    }

    private static List<SortSpec> toSorts(SortRuleRequest sort) {
        if (sort == null || sort.orders() == null || sort.orders().isEmpty()) {
            return List.of();
        }

        List<SortSpec> sorts = new ArrayList<>();
        for (SortSpec it : sort.orders()) {
            if (it == null) {
                continue;
            }
            String field = it.field();
            if (field == null || field.isBlank()) {
                continue;
            }
            sorts.add(it);
        }
        return sorts;
    }
}
