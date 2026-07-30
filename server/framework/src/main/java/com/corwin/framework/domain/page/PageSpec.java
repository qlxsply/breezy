package com.corwin.framework.domain.page;

import java.util.List;

/**
 * Pagination specification with page number, page size, and sort criteria.
 * <p>
 * Provides factory methods and defaults ({@value #DEFAULT_PAGE_NO}, {@value #DEFAULT_PAGE_SIZE}).
 * Page numbers are 1-based; values less than 1 are normalized to the default.
 *
 * @param pageNo   the requested page number (1-based)
 * @param pageSize the requested page size
 * @param sorts    the sort specifications (immutable copy stored)
 * @author Corwin 2026/3/30
 */
public record PageSpec(
        int pageNo,
        int pageSize,
        List<SortSpec> sorts
) {
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public PageSpec {
        if (pageNo < 1) {
            pageNo = DEFAULT_PAGE_NO;
        }
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        sorts = (sorts == null || sorts.isEmpty()) ? List.of() : List.copyOf(sorts);
    }

    public static PageSpec of(Integer pageNo, Integer pageSize, List<SortSpec> sorts) {
        int resolvedPageNo = (pageNo == null) ? DEFAULT_PAGE_NO : pageNo;
        int resolvedPageSize = (pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
        return new PageSpec(resolvedPageNo, resolvedPageSize, sorts);
    }

    public static PageSpec ensure(PageSpec spec) {
        return spec == null ? PageSpec.of(null, null, List.of()) : spec;
    }

    public PageSpec withSorts(List<SortSpec> sorts) {
        return new PageSpec(pageNo, pageSize, sorts);
    }

    public PageSpec clearSorts() {
        return withSorts(List.of());
    }

}
