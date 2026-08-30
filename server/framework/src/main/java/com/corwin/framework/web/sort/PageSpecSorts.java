package com.corwin.framework.web.sort;

import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.StrUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates and resolves sort specifications against the current endpoint's {@link SortRule},
 * mapping frontend field names to SQL column names.
 *
 * <p>If sorting is disabled for the endpoint, all sort criteria are stripped. If the request does
 * not provide any sort, the endpoint's default sort is applied.
 *
 * @author Corwin 2026/7/29
 */
public final class PageSpecSorts {

  private PageSpecSorts() {}

  public static PageSpec apply(PageSpec spec) {
    PageSpec resolved = PageSpec.ensure(spec);
    SortRule rule = SortRuleContextHolder.get();
    if (rule == null) {
      return resolved;
    }
    if (!rule.enabled()) {
      return resolved.clearSorts();
    }

    Map<String, String> fields = toFieldMap(rule.allowed());
    List<SortSpec> source = resolved.sorts().isEmpty() ? rule.defaults() : resolved.sorts();
    if (source.isEmpty()) {
      return resolved.clearSorts();
    }
    return resolved.withSorts(resolveSorts(source, fields));
  }

  private static Map<String, String> toFieldMap(List<SortableField> allowed) {
    Map<String, String> result = new LinkedHashMap<>();
    for (SortableField item : allowed) {
      if (item == null) {
        continue;
      }
      String field = StrUtil.trimToNull(item.field());
      String column = StrUtil.trimToNull(item.column());
      if (field == null || column == null) {
        continue;
      }
      result.put(field, column);
    }
    return result;
  }

  private static List<SortSpec> resolveSorts(List<SortSpec> sorts, Map<String, String> fields) {
    List<SortSpec> result = new ArrayList<>();
    for (SortSpec sort : sorts) {
      if (sort == null || StrUtil.trimToNull(sort.field()) == null) {
        continue;
      }
      String column = fields.get(sort.field().trim());
      if (column == null) {
        throw new BizException(
            "Unsupported sort field: " + sort.field(), BaseError.INVALID_PARAMETER);
      }
      result.add(new SortSpec(column, sort.direction()));
    }
    return result;
  }
}
