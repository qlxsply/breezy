package com.corwin.framework.domain.page;

import java.util.List;

/**
 * Generic paginated data result.
 *
 * <p>Carries the current page number, page size, element count metadata, and the actual element
 * list for the current page.
 *
 * @param <T> the element type
 * @param pageNo the current page number (1-based)
 * @param pageSize the page size
 * @param numberOfElements the number of elements in the current page
 * @param totalPages the total number of pages
 * @param totalElements the total number of elements across all pages
 * @param elements the element list for the current page
 * @author Corwin 2026/3/30
 */
public record PageData<T>(
    int pageNo,
    int pageSize,
    int numberOfElements,
    int totalPages,
    int totalElements,
    List<T> elements) {

  public PageData {
    elements = (elements == null || elements.isEmpty()) ? List.of() : List.copyOf(elements);
  }

  public static <T> PageData<T> of(PageSpec spec, long count, List<T> list) {
    int pageNo = (spec == null) ? PageSpec.DEFAULT_PAGE_NO : spec.pageNo();
    int pageSize = (spec == null) ? PageSpec.DEFAULT_PAGE_SIZE : spec.pageSize();
    return of(pageNo, pageSize, count, list);
  }

  public static <T> PageData<T> of(int pageNo, int pageSize, long count, List<T> list) {
    int totalElements = safeLongToInt(count);
    List<T> elements = (list == null || list.isEmpty()) ? List.of() : List.copyOf(list);
    int numberOfElements = elements.size();
    int totalPages =
        (totalElements <= 0 || pageSize <= 0) ? 0 : ((totalElements + pageSize - 1) / pageSize);
    return new PageData<>(pageNo, pageSize, numberOfElements, totalPages, totalElements, elements);
  }

  private static int safeLongToInt(long v) {
    if (v <= 0) {
      return 0;
    }
    if (v >= Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) v;
  }
}
