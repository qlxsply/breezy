package com.corwin.framework.web.response;

import com.corwin.framework.domain.page.PageData;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Generic paginated response wrapper.
 *
 * <p>Converts a {@link com.corwin.framework.domain.page.PageData} into a frontend-friendly
 * structure with page metadata and element list.
 *
 * @param <T> the element type
 * @author Corwin 2026/1/8
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResult<T> {
  /** Current page number (1-based) */
  private final int pageNo;

  /** Page size as requested */
  private final int pageSize;

  /** Number of elements in the current page */
  private final int numberOfElements;

  /** Total number of pages */
  private final int totalPages;

  /** Total number of elements across all pages */
  private final int totalElements;

  /** The current page's element list */
  private final List<T> elements;

  public static <T> PageResult<T> of(Integer pageNo, Integer pageSize, long count, List<T> list) {
    int totalElements = safeLongToInt(count);
    List<T> elements = (list == null) ? Collections.emptyList() : list;
    pageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
    pageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
    int numberOfElements = elements.size();
    int totalPages = (totalElements <= 0) ? 0 : ((totalElements + pageSize - 1) / pageSize);

    return new PageResult<>(
        pageNo, pageSize, numberOfElements, totalPages, totalElements, elements);
  }

  public static <T> PageResult<T> of(PageData<T> page) {
    if (page == null) {
      return new PageResult<>(1, 20, 0, 0, 0, Collections.emptyList());
    }
    return new PageResult<>(
        page.pageNo(),
        page.pageSize(),
        page.numberOfElements(),
        page.totalPages(),
        page.totalElements(),
        page.elements());
  }

  public static <T, R> PageResult<R> of(PageData<T> page, Function<? super T, ? extends R> mapper) {
    if (page == null) {
      return new PageResult<>(1, 20, 0, 0, 0, Collections.emptyList());
    }
    List<R> elements =
        page.elements().stream().map(mapper).collect(Collectors.toUnmodifiableList());
    return new PageResult<>(
        page.pageNo(),
        page.pageSize(),
        page.numberOfElements(),
        page.totalPages(),
        page.totalElements(),
        elements);
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
