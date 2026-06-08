/**
 * @author Corwin 2026/3/30
 */
package com.corwin.framework.persistence.jpa;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JpaPageMapper {

    private JpaPageMapper() {
    }

    public static Pageable toPageable(PageSpec spec) {
        PageSpec resolved = (spec == null) ? PageSpec.of(null, null, List.of()) : spec;
        Sort sort = toSort(resolved);
        return PageRequest.of(resolved.pageNo() - 1, resolved.pageSize(), sort);
    }

    public static Sort toSort(PageSpec spec) {
        if (spec == null || spec.sorts().isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (SortSpec it : spec.sorts()) {
            if (it == null) {
                continue;
            }
            String field = it.field();
            if (field == null || field.isBlank()) {
                continue;
            }
            Sort.Direction dir = SortDirection.DESC.equals(it.direction()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(dir, field));
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    public static <T> PageData<T> toPageData(Page<T> page) {
        if (page == null) {
            return PageData.of(PageSpec.DEFAULT_PAGE_NO, PageSpec.DEFAULT_PAGE_SIZE, 0, List.of());
        }
        return new PageData<>(page.getNumber() + 1, page.getSize(), page.getNumberOfElements(), page.getTotalPages(),
                safeLongToInt(page.getTotalElements()), page.getContent());
    }

    public static <T, R> PageData<R> toPageData(Page<T> page, Function<? super T, ? extends R> mapper) {
        if (page == null) {
            return PageData.of(PageSpec.DEFAULT_PAGE_NO, PageSpec.DEFAULT_PAGE_SIZE, 0, List.of());
        }
        List<R> elements = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return new PageData<>(page.getNumber() + 1, page.getSize(), page.getNumberOfElements(), page.getTotalPages(),
                safeLongToInt(page.getTotalElements()), elements);
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
